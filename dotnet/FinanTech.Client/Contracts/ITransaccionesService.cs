using System.ServiceModel;
using FinanTech.WcfService.CoreWcf;

namespace FinanTech.Client.Contracts;

/// <summary>
/// Contrato del cliente WCF (atributos System.ServiceModel).
/// Debe coincidir con el contrato CoreWCF publicado por el servicio.
/// </summary>
[ServiceContract(Namespace = "urn:finantech:transacciones:v1")]
public interface ITransaccionesService
{
    [OperationContract]
    [FaultContract(typeof(ErrorFinanciero))]
    TransaccionResponse ProcesarTransaccion(TransaccionRequest request);

    [OperationContract]
    [FaultContract(typeof(ErrorFinanciero))]
    TransaccionResponse ConsultarTransaccion(string idTransaccion);
}
