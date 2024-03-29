package nc.liat6.frame.db.custom.csv;

import java.io.File;
import nc.liat6.frame.db.connection.ConnVar;
import nc.liat6.frame.db.connection.impl.SuperConnVarProvider;
import nc.liat6.frame.locale.L;
import nc.liat6.frame.locale.LocaleFactory;
import nc.liat6.frame.log.Logger;
import nc.liat6.frame.util.Stringer;

/**
 * CSV连接变量提供器
 * 
 * @author 6tail
 * 
 */
public class CsvConnVarProvider extends SuperConnVarProvider{

	@Override
	protected void registDriver(String driver){
		File dir = new File(this.setting.getDbName());
		if(!dir.exists()&&!dir.isDirectory()){
			dir.mkdirs();
			Logger.getLog().debug(Stringer.print("?:?",L.get(LocaleFactory.locale,"db.regist_driver"),dir.getAbsolutePath()));
		}
	}

	public ConnVar getConnVar(){
		ConnVar cv = new ConnVar();
		cv.setDbType(setting.getDbType());
		cv.setAlias(setting.getAlias());
		CsvConnection cc = new CsvConnection();
		cc.setConnVar(cv);
		cv.setConnection(cc);
		cv.setSetting(setting);
		return cv;
	}

	@Override
	public boolean support(String connType){
		return "csv".equalsIgnoreCase(connType);
	}

}
