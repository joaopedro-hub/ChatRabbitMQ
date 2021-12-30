/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.chatrabbit;

import com.google.protobuf.*;
import com.google.gson.*;
import com.rabbitmq.client.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.file.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Scanner;

/**
 *
 * @author João Pedro
 */
public class EmiRecep {
    private static String usuarioAux1 = "";//usuario da fila
    private static String usuarioAux2 = "";//usuario para a qual a mensagem é enviada
    public static void main(String[] argv) throws Exception {
        Scanner scan = new Scanner(System.in);
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(""); // Alterar
        factory.setUsername(""); // Alterar
        factory.setPassword(""); // Alterar
        factory.setVirtualHost("/");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        Channel channelArq = connection.createChannel();
        MensagemProto.Mensagem.Builder mensagemPB = MensagemProto.Mensagem.newBuilder();

//-------------------------------------------------------------------------------------//
        String usuarioP, userAux="", mensagem=""
                ,nomeGrupo = "",usuarioArq,nomeGrupoArq = "";
        Boolean status = true;
        System.out.print("User:");
        usuarioP = scan.nextLine();
        usuarioArq = usuarioP.concat("-files");
        usuarioAux1 = usuarioP;
        

        //(queue-name, durable, exclusive, auto-delete, params); 
        channel.queueDeclare(usuarioP, false, false, false, null);
        channelArq.queueDeclare(usuarioArq, false, false, false, null);

            //--------------------------------------------------------------------------------------------------//
            //parte receptor
            Consumer consumer = new DefaultConsumer(channel) {
                
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    Scanner aux = new Scanner(System.in);
                    try {
                        MensagemProto.Mensagem msg = MensagemProto.Mensagem.parseFrom(body);
                        String emissor,data,hora,conteudo,grupo;
                        emissor = msg.getEmissor();
                        data = msg.getData();
                        hora = msg.getHora();
                        conteudo = msg.getCorpo().toStringUtf8();
                        grupo = msg.getGrupo();
                        
                        if(grupo.isEmpty()){
                            System.out.println("");
                            System.out.println("(" + data + " às " + hora + ") " + emissor + " diz: " + conteudo);
                            
                            System.out.print(usuarioAux2 + ">>");//usuario que recebeu
                        }else{
                            System.out.println("");
                            System.out.println("(" + data + " às " + hora + ") " + emissor + grupo + " diz: " + conteudo);
                            
                            System.out.print(usuarioAux2 + ">>");//usuario que recebeu
                        }
                        
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    
                }
            };
            channel.basicConsume(usuarioP, true, consumer);
            
        //Parte Do Arquivo
        
        //--------------------------------------------------------------------------------------------------//
        Consumer consumerArq = new DefaultConsumer(channelArq) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                try {
                    MensagemProto.Mensagem msg = MensagemProto.Mensagem.parseFrom(body);
                    String emissor,data,hora,grupo,tpMine,nome;
                    
                        emissor = msg.getEmissor();
                        data = msg.getData();
                        hora = msg.getHora();
                        grupo = msg.getGrupo();
                        tpMine = msg.getTipo();
                        nome = msg.getNome();
                        byte[] conteudo = msg.getCorpo().toByteArray();
                        
                        File diretorio = new File("media/downloads/");
                        diretorio.mkdirs();
                        File entrada = new File(diretorio,nome);
                        FileOutputStream saida1 = new FileOutputStream(entrada);
                        BufferedOutputStream saida2 = new BufferedOutputStream(saida1);
                        saida2.write(conteudo);
                        saida2.flush();
                        saida2.close();
                        
                        System.out.println("("+data+" às "+hora+") Arquivo \""+nome+"\"recebido de "+emissor);
                        System.out.print("@"+emissor+">>");
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        };
        channelArq.basicConsume(usuarioArq, true, consumerArq);
        //--------------------------------------------------------------------------------------------------//
        System.out.print(">>");
        
        
        while(status){
            
            try {
                mensagem = scan.nextLine();

                if (mensagem.startsWith("@") || mensagem.startsWith("#")){
                    userAux = mensagem;//cria a fila em nome do usuario
                    System.out.print(userAux+">>");
                    usuarioAux2 = userAux;
                }
                if (mensagem.isEmpty()) {
                    System.out.print(userAux + ">>");
                }

                if (mensagem.equals("sair")) {
                    status = false;
                }
                
                if(mensagem.startsWith("!newGroup")){
                    nomeGrupo = mensagem.substring(10);
                    nomeGrupoArq = nomeGrupo.concat("-files");
                    
                    channel.exchangeDeclare(nomeGrupo, "fanout");
                    channel.queueBind(usuarioP, nomeGrupo, "");
                    
                    channelArq.exchangeDeclare(nomeGrupoArq, "fanout");
                    channelArq.queueBind(usuarioArq, nomeGrupoArq, "");
                    System.out.print(userAux + ">>");
                }
                
                if(mensagem.startsWith("!addUser")){
                    channel.queueBind(mensagem.split("\\s")[1], mensagem.split("\\s")[2], "");
                    channelArq.queueBind(mensagem.split("\\s")[1].concat("-files"), mensagem.split("\\s")[2].concat("-files"), "");
                    System.out.print(userAux + ">>");
                }
                
                
                if(mensagem.startsWith("!delFromGroup")){
                    channel.queueUnbind(mensagem.split("\\s")[1], mensagem.split("\\s")[2], "");
                    channelArq.queueUnbind(mensagem.split("\\s")[1].concat("-files"), mensagem.split("\\s")[2].concat("-files"),"");
                    System.out.print(userAux + ">>");
                }
                
                if(mensagem.startsWith("!removeGroup")){
                    nomeGrupo = mensagem.substring(13);
                    nomeGrupoArq = nomeGrupo.concat("-files");
                    channel.exchangeDelete(nomeGrupo);
                    channelArq.exchangeDelete(nomeGrupoArq);
                    System.out.print(userAux + ">>");
                }
                
                if(mensagem.startsWith("!upload")){
                    String url = mensagem.substring(8);
                    String AuxUrl = "";
                    if(url.startsWith("/")){
                        AuxUrl = url.substring(1);
                    }else{
                        AuxUrl = url;
                    }
                    
                    Date dataHora = new Date();
                    String data = new SimpleDateFormat("dd/MM/yyyy").format(dataHora);
                    String hora = new SimpleDateFormat("HH:mm").format(dataHora);
                    
                    System.out.println("Enviando \""+AuxUrl+"\" para "+ userAux+"...");
                    System.out.print(userAux + ">>");
                    
                   
                    ThreadEmissor enviando = new ThreadEmissor(usuarioP, data, hora,userAux, AuxUrl);
                    enviando.main(new String[]{});
                }
                
                if(mensagem.startsWith("!listUsers")){
                    String grupo = mensagem.substring(11);
                    String caminho = "/api/exchanges/%2f/"+grupo+"/bindings/source";//Uma lista de todas as ligações nas quais uma determinada exchange é a fonte.
                    RESTClient rest = new RESTClient(caminho);
                    rest.main(new String[]{});
                    System.out.print(userAux + ">>");
                }
                
                if(mensagem.startsWith("!listGroups")){
                    String nomeGr = userAux.substring(1);
                    String caminho = "/api/queues/%2f/"+nomeGr+"/bindings";//Uma lista de todas as ligações em uma determinada fila.
                    RESTClient rest = new RESTClient(caminho);
                    rest.main(new String[]{});
                    System.out.print(userAux + ">>");
                }
                if(!mensagem.substring(0,1).matches("\\p{Punct}")){
                    
                    if(userAux.startsWith("@")) {
                        
                        Date dataHora = new Date();
                        String data = new SimpleDateFormat("dd/MM/yyyy").format(dataHora);
                        String hora = new SimpleDateFormat("HH:mm").format(dataHora);
                        
                        mensagemPB.setEmissor(usuarioP);
                        mensagemPB.setData(data);
                        mensagemPB.setHora(hora);
                        mensagemPB.setTipo("text/plain");
                        mensagemPB.setCorpo(ByteString.copyFromUtf8(mensagem));
                        MensagemProto.Mensagem empacotador = mensagemPB.build();
                        byte[] damper = empacotador.toByteArray();
                        //novaM = "("+data + " às " + hora+") " + usuarioP + " diz: " + mensagem;
                        channel.basicPublish("",userAux.substring(1), null, damper);
                        System.out.print(userAux + ">>");
                    }else if(userAux.startsWith("#")){
                        Date dataHora = new Date();
                        String data = new SimpleDateFormat("dd/MM/yyyy").format(dataHora);
                        String hora = new SimpleDateFormat("HH:mm").format(dataHora);

                        mensagemPB.setEmissor(usuarioP);
                        mensagemPB.setData(data);
                        mensagemPB.setHora(hora);
                        mensagemPB.setTipo("text/plain");
                        mensagemPB.setCorpo(ByteString.copyFromUtf8(mensagem));
                        mensagemPB.setGrupo(userAux);
                        MensagemProto.Mensagem empacotador = mensagemPB.build();
                        byte[] damper = empacotador.toByteArray();
                        
                        channel.basicPublish(userAux.substring(1), "", null, damper);
                        System.out.print(userAux + ">>");
                    }
                    
                }
                
            } catch (Exception e) {
                System.out.println("Algo deu errado.Tente novamente!");
                
                if(userAux.isEmpty()){
                    System.out.print(">>");
                }else{
                    System.out.print(userAux+">>");
                }
            }
            
            
        }
                    
        

        channel.close();
        connection.close();
    }
}
