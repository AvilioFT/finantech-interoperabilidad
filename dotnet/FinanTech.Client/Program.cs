using System.Diagnostics;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.ServiceModel;
using System.Text;
using System.Text.Json;
using FinanTech.Client.Contracts;
using FinanTech.WcfService.CoreWcf;

var javaBaseUrl = args.Length > 0 ? args[0] : "http://localhost:8080";
var wcfServiceUrl = args.Length > 1 ? args[1] : "http://localhost:8081/TransaccionService.svc";

var jsonOptions = new JsonSerializerOptions(JsonSerializerDefaults.Web);

Console.WriteLine("=== FinanTech S.A. - Cliente .NET ===");
Console.WriteLine($"Java: {javaBaseUrl}");
Console.WriteLine($"WCF : {wcfServiceUrl}");

using var http = new HttpClient { BaseAddress = new Uri(javaBaseUrl) };

// 1) Login contra el servicio Java para obtener el JWT
var loginResponse = await http.PostAsJsonAsync("/api/v1/auth/token",
    new { username = "cliente", password = "Clave123!" }, jsonOptions);
loginResponse.EnsureSuccessStatusCode();

using var tokenDoc = JsonDocument.Parse(await loginResponse.Content.ReadAsStringAsync());
var accessToken = tokenDoc.RootElement.GetProperty("accessToken").GetString()!;
http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);
Console.WriteLine($"[Java] JWT obtenido ({accessToken.Length} caracteres)");

// 2) POST de transaccion con Idempotency-Key
var transaccion = new
{
    cuentaOrigen = "001-1234567890",
    cuentaDestino = "002-0987654321",
    monto = 1500.00m,
    moneda = "PEN",
    descripcion = "Prueba .NET -> Java"
};

using var request = new HttpRequestMessage(HttpMethod.Post, "/api/v1/transacciones")
{
    Content = new StringContent(JsonSerializer.Serialize(transaccion, jsonOptions), Encoding.UTF8, "application/json")
};
request.Headers.Add("Idempotency-Key", Guid.NewGuid().ToString());

var cronometro = Stopwatch.StartNew();
var transaccionResponse = await http.SendAsync(request);
cronometro.Stop();

var transaccionBody = await transaccionResponse.Content.ReadAsStringAsync();
Console.WriteLine($"[Java] POST /api/v1/transacciones -> {(int)transaccionResponse.StatusCode} en {cronometro.ElapsedMilliseconds} ms");
Console.WriteLine(transaccionBody);
transaccionResponse.EnsureSuccessStatusCode();

// 3) GET de la transaccion creada
using var transaccionDoc = JsonDocument.Parse(transaccionBody);
var idTransaccion = transaccionDoc.RootElement.GetProperty("idTransaccion").GetString();
var getResponse = await http.GetAsync($"/api/v1/transacciones/{idTransaccion}");
Console.WriteLine($"[Java] GET /api/v1/transacciones/{idTransaccion} -> {(int)getResponse.StatusCode}");
Console.WriteLine(await getResponse.Content.ReadAsStringAsync());

// 4) Invocacion SOAP al servicio WCF (basicHttpBinding)
var channelFactory = new ChannelFactory<ITransaccionesService>(
    new BasicHttpBinding(),
    new EndpointAddress(wcfServiceUrl));
var canal = channelFactory.CreateChannel();

try
{
    var wcfRequest = new TransaccionRequest
    {
        CuentaOrigen = "001-1234567890",
        CuentaDestino = "002-0987654321",
        Monto = 1500m,
        Moneda = "PEN",
        Descripcion = "Prueba .NET -> WCF"
    };

    var wcfResponse = canal.ProcesarTransaccion(wcfRequest);
    Console.WriteLine($"[WCF] {wcfResponse.IdTransaccion} | {wcfResponse.Estado} | {wcfResponse.Mensaje}");

    var consulta = canal.ConsultarTransaccion(wcfResponse.IdTransaccion);
    Console.WriteLine($"[WCF] Consulta: {consulta.IdTransaccion} | {consulta.Estado}");
}
catch (FaultException<ErrorFinanciero> fault)
{
    Console.WriteLine($"[WCF] Fault {fault.Detail.Codigo}: {fault.Detail.Mensaje} (traceId={fault.Detail.TraceId})");
}
finally
{
    ((IClientChannel)canal).Close();
    channelFactory.Close();
}

Console.WriteLine("=== Fin ===");
