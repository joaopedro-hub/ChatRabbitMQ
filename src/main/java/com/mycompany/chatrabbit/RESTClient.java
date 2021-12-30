
package com.mycompany.chatrabbit;

/**
 *
 * @author João Pedro
 */
import com.google.gson.*;
import com.rabbitmq.client.impl.StrictExceptionHandler;
import java.io.*;
import java.util.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.*;
public class RESTClient {
    private static String caminho;

    public RESTClient(String caminho) {
        this.caminho = caminho;
    }

    public static String getCaminho() {
        return caminho;
    }

    public static String getConexao(){
        String json = new String();
        
        try {
            String usuario = "admin";
            String senha = "s8g2ad51";
            
            String UsuarioSenha = usuario + ":" + senha;
            String Autorizador = "Authorization";
            String AutorizaValor = "Basic " + java.util.Base64.getEncoder().encodeToString(
            UsuarioSenha.getBytes());
            
            //Requisitando
            
            String linkResource = "http://Amqp-LB-3268b08a3d5c9686.elb.us-east-1.amazonaws.com";//modificar
            
            Client cliente = ClientBuilder.newClient();
            Response resposta =  cliente.target(linkResource).path(getCaminho()).request(
            MediaType.APPLICATION_JSON).header(Autorizador,AutorizaValor).get();
            
            if(resposta.getStatus() == 200){
                json = resposta.readEntity(String.class);
                System.out.println(json);
            }else{
                System.out.println("Falha na Conexao. Tente Novamente.");
                System.out.println(resposta.getStatus());
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }
    
    public static void LerConexao(String json){
        
        try {
            JsonArray saida = (new Gson().fromJson(json, JsonArray.class));
            
            String chaveResp = saida.get(0).getAsJsonObject().get("source").getAsString();
            if(chaveResp.equals("")){
                
                for(int i = 1; i < saida.size(); i++){
                    System.out.println(saida.get(i).getAsJsonObject().get("source").getAsString());
                    
                    if(i != saida.size()-1){
                        System.out.println(", ");
                    }
                }
            }else{
                
                for(int i = 0; i < saida.size(); i++){
                    System.out.println(saida.get(i).getAsJsonObject().get("destination").getAsString());
                    
                    if(i != saida.size()-1){
                            System.out.println(", ");
                        }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String json = new String();
        json = getConexao();
        LerConexao(json);
    }
    
}
