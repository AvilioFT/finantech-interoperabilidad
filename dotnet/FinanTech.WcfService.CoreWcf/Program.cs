using CoreWCF;
using CoreWCF.Configuration;
using CoreWCF.Description;
using FinanTech.WcfService.CoreWcf;
using FinanTech.WcfService.CoreWcf.Contracts;
using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.DependencyInjection;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddServiceModelServices();
builder.Services.AddServiceModelMetadata();

var app = builder.Build();
app.Urls.Add("http://localhost:8081");

app.UseServiceModel(serviceBuilder =>
{
    serviceBuilder.AddService<TransaccionesService>();
    serviceBuilder.AddServiceEndpoint<TransaccionesService, ITransaccionesService>(
        new BasicHttpBinding(), "/TransaccionService.svc");
});

var serviceMetadataBehavior = app.Services.GetRequiredService<ServiceMetadataBehavior>();
serviceMetadataBehavior.HttpGetEnabled = true;

app.Run();
