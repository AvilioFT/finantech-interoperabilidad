using CoreWCF;

namespace FinanTech.WcfService.CoreWcf.Contracts;

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
