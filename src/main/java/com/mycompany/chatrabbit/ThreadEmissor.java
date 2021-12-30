package com.mycompany.chatrabbit;

/**
 *
 * @author João Pedro
 */
import com.google.protobuf.*;
import com.google.gson.*;
import com.rabbitmq.client.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.nio.file.*;

public class ThreadEmissor {
    private static String emissor;
    private static String data;
    private static String hora;
    private static String usuario;
    private static String caminho;
    
    public ThreadEmissor(String emi,String dat,String ho,String us,String cam){
        this.emissor = emi;
        this.data = dat;
        this.hora = ho;
        this.usuario = us;
        this.caminho = cam;
    }

    public static String getEmissor() {
        return emissor;
    }

    public static String getData() {
        return data;
    }

    public static String getHora() {
        return hora;
    }

    public static String getUsuario() {
        return usuario;
    }

    public static String getCaminho() {
        return caminho;
    }
    
    private static Runnable enviar = new Runnable(){
        
        @Override
        public void run(){
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("Amqp-LB-3268b08a3d5c9686.elb.us-east-1.amazonaws.com"); // Alterar
            factory.setUsername("admin"); // Alterar
            factory.setPassword("s8g2ad51"); // Alterar
            factory.setVirtualHost("/");
            
            try {
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
                
                String[] splitUrl = getCaminho().split("\\p{Punct}");
                int tam = splitUrl.length;
                String arquivo = splitUrl[tam-2].concat(".").concat(splitUrl[tam-1]);
                String userArq1 = getUsuario().substring(1).concat("-files");
                

                
                File file = new File(getCaminho());
                int tam2 = (int)file.length();
                byte[] arq = new byte[tam2];
                FileInputStream entrada = new FileInputStream(file);
                entrada.read(arq);
                entrada.close();
                
                MensagemProto.Mensagem.Builder mensagemPB = MensagemProto.Mensagem.newBuilder();
                mensagemPB.setEmissor(getEmissor());
                mensagemPB.setData(getData());
                mensagemPB.setHora(getHora());
                if(getUsuario().startsWith("#")){
                    mensagemPB.setGrupo(getUsuario());
                }else{
                    mensagemPB.setGrupo("");
                }
                mensagemPB.setCorpo(ByteString.copyFrom(arq));
                mensagemPB.setNome(arquivo);
                
                MensagemProto.Mensagem empacotador = mensagemPB.build();
                byte[] damper = empacotador.toByteArray();
                
                
                if(getUsuario().startsWith("@")){
                    channel.basicPublish("", userArq1, null, damper);
                    System.out.println("Arquivo \"" + getCaminho() +"\"foi enviado para "+getUsuario());
                    System.out.print(getUsuario()+">>");
                }else{
                    channel.basicPublish(userArq1,"", null, damper);
                    System.out.println("Arquivo \"" + getCaminho() +"\"foi enviado para "+getUsuario());
                    System.out.print(getUsuario()+">>");
                }
            } catch (Exception e) {
                System.out.println("Fallha ao enviar oarquivo");
                System.out.println(e.getMessage());
                System.out.println(getUsuario()+"==>>");
            }
        }
    };
    
    public static void main(String[] args) {
        new Thread(enviar).start();
    }
}
