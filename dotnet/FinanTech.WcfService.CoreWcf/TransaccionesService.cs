using System.Collections.Concurrent;
using CoreWCF;

namespace FinanTech.WcfService.CoreWcf;

[ServiceBehavior(Namespace = "urn:finantech:transacciones:v1")]
public class TransaccionesService : Contracts.ITransaccionesService
{
    // Almacen de demostracion compartido entre llamadas (en produccion seria una base de datos).
    private static readonly ConcurrentDictionary<string, TransaccionResponse> Transacciones = new();

    public TransaccionResponse ProcesarTransaccion(TransaccionRequest request)
    {
        bool fallida = request.Monto <= 0;

        var response = new TransaccionResponse
        {
            IdTransaccion = "TX-" + DateTime.UtcNow.ToString("yyyyMMddHHmmss") + "-" + Guid.NewGuid().ToString("N"),
            CuentaOrigen = request.CuentaOrigen,
            CuentaDestino = request.CuentaDestino,
            Monto = request.Monto,
            Moneda = string.IsNullOrWhiteSpace(request.Moneda) ? "PEN" : request.Moneda,
            Estado = fallida ? "FALLIDA" : "APROBADA",
            FechaHora = DateTime.UtcNow,
            Mensaje = fallida ? "Transaccion fallida: el monto debe ser mayor que cero"
                              : "Transaccion procesada"
        };

        Transacciones[response.IdTransaccion] = response;

        if (fallida)
        {
            throw new FaultException<ErrorFinanciero>(
                new ErrorFinanciero
                {
                    Codigo = "VAL-001",
                    Mensaje = "El monto debe ser mayor que cero",
                    TraceId = Guid.NewGuid().ToString()
                },
                "Solicitud invalida");
        }

        return response;
    }

    public TransaccionResponse ConsultarTransaccion(string idTransaccion)
    {
        if (Transacciones.TryGetValue(idTransaccion, out var response))
        {
            return response;
        }

        throw new FaultException<ErrorFinanciero>(
            new ErrorFinanciero
            {
                Codigo = "NOT-001",
                Mensaje = "Transaccion no encontrada",
                TraceId = Guid.NewGuid().ToString()
            },
            "Recurso no encontrado");
    }
}
