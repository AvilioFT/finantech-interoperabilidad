using System.Runtime.Serialization;

namespace FinanTech.WcfService.CoreWcf;

[DataContract(Namespace = "urn:finantech:transacciones:v1")]
public class ErrorFinanciero
{
    [DataMember(Name = "codigo")]
    public string Codigo { get; set; } = string.Empty;

    [DataMember(Name = "mensaje")]
    public string Mensaje { get; set; } = string.Empty;

    [DataMember(Name = "traceId")]
    public string TraceId { get; set; } = Guid.NewGuid().ToString();
}
