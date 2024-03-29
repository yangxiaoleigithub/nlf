package nc.liat6.frame.log;

/**
 * 日志适配器接口
 * 
 * @author 6tail
 * 
 */
public interface ILogAdapter{

  /**
   * 是否支持该日志工具
   * 
   * @return true/false 支持/不支持
   */
  public boolean isSupported();

  /**
   * 获取日志记录器
   * 
   * @param klass 类名
   * @return 日志记录接口
   */
  public ILog getLog(String klass);
}
