using System.Runtime.Serialization;

namespace FinanTech.WcfService.CoreWcf;

[DataContract(Namespace = "urn:finantech:transacciones:v1")]
public class TransaccionRequest
{
    [DataMember(Name = "idTransaccion")]
    public string IdTransaccion { get; set; } = string.Empty;

    [DataMember(Name = "cuentaOrigen")]
    public string CuentaOrigen { get; set; } = string.Empty;

    [DataMember(Name = "cuentaDestino")]
    public string CuentaDestino { get; set; } = string.Empty;

    [DataMember(Name = "monto")]
    public decimal Monto { get; set; }

    [DataMember(Name = "moneda")]
    public string Moneda { get; set; } = "PEN";

    [DataMember(Name = "descripcion")]
    public string Descripcion { get; set; } = string.Empty;
}
