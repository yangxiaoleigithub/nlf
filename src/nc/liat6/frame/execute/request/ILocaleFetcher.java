package nc.liat6.frame.execute.request;

import java.util.Locale;

/**
 * 客户端locale获取接口
 * 
 * @author 6tail
 * 
 */
public interface ILocaleFetcher{

  /**
   * 获取客户端locale
   * 
   * @return locale
   */
  public Locale getLocale();

  /**
   * 获取客户端locale字符串
   * 
   * @return locale字符串，如zh-CN
   */
  public String getLocaleString();

  /**
   * 设置locale
   * 
   * @param locale locale
   */
  public void setLocale(Locale locale);

  /**
   * 设置locale
   * 
   * @param locale locale字符串，如zh-CN
   */
  public void setLocale(String locale);
}
