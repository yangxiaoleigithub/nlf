package nc.liat6.frame.rmi.server.request;

public class RmiRequest{

  private String ip;
  private int port;

  public String getIp(){
    return ip;
  }

  public void setIp(String ip){
    this.ip = ip;
  }

  public int getPort(){
    return port;
  }

  public void setPort(int port){
    this.port = port;
  }
}
