package nc.liat6.frame.db.setting;

import nc.liat6.frame.db.entity.Bean;

/**
 * 连接设置提供接口
 * 
 * @author 6tail
 * 
 */
public interface IDbSettingProvider{

  /**
   * 是否支持指定连接类型
   * 
   * @param type 连接类型
   * @return true/false 支持/不支持
   */
  public boolean support(String type);

  /**
   * 获取连接配置
   * 
   * @param o 配置文件转换的Bean
   * @return 连接配置
   */
  public IDbSetting getDbSetting(Bean o);
}
