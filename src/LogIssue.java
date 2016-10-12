/**
 * This Class using for import UT result to Redmine
 *
 * @author TranTTB
 * @version 1.0
 * @since 2016-08-20
 */
public class LogIssue {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String uri;
		String apiKey;
		String folderPath;
		String projectKey;
		Common common;
		
		uri = args[0];
		apiKey = args[1];
		folderPath = args[2];
		projectKey = args[3];
		common = new Common(uri, apiKey);
		common.readResult(folderPath, projectKey);
	}
}
