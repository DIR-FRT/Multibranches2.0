import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.taskadapter.redmineapi.IssueManager;
import com.taskadapter.redmineapi.ProjectManager;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueCategory;
import com.taskadapter.redmineapi.bean.IssueFactory;
import com.taskadapter.redmineapi.bean.IssueStatus;
import com.taskadapter.redmineapi.bean.Project;
import com.taskadapter.redmineapi.bean.User;
import com.taskadapter.redmineapi.bean.Version;
import com.taskadapter.redmineapi.bean.VersionFactory;

/**
 * This Class using for import UT result to Redmine
 *
 * @author TranTTB
 * @version 1.0
 * @since 2016-08-20
 */
public class Common {
	String uri;
	String apiKey;
	String trackerName;
	int statusID;
	RedmineManager redmineMng;
	int DEFAULT_VERSION = 512;
	int DEFAULT_CATEGORY = 673;
	String SYMBOL_QUOTA = "";
	String SYMBOL_DOT = ".";
	String SYMBOL_PAREN_LEFT = "[";
	String SYMBOL_PAREN_RIGHT = "]";
	String SYMBOL_HAIFUN = "_";
	String TEST_RESULT_TYPE = "xml";
	String TESTCASE_ATTRIBUTE_NAME = "name";
	String TESTCASE_ATTRIBUTE_TIME = "time";
	String TRACKER_TEST_RESULT = "Test Result";
	String PASSED = "Passed";
	String FAILED = "Failed";
	String RELATION_TYPE = "relates";
	String TESTCASE = "testcase";
	String UT_CUSTOMIZE_FIELD_TEST_RESULT = "Test Result";
	String TEST_COMPLETED = "Testcompleted";
	String TIME_REGEX = "yyyyMMddHHmmss";
	String EXECUTE_TIME = "Excuted Time : ";

	public Common(String uri, String apiKey) {
		// TODO Auto-generated constructor stub
		this.uri = uri;
		this.apiKey = apiKey;
		redmineMng = RedmineManagerFactory.createWithApiKey(uri, apiKey);
		statusID = getStatusIdByName(TEST_COMPLETED);
		trackerName = TRACKER_TEST_RESULT;
	}

	/**
	 * This method is used to read UT result xml file
	 * 
	 * @param folderPath
	 * @param projectKey
	 * @param projectKey2
	 * @param folderPath2
	 */
	public void readResult(String folderPath, String projectKey) {

		int issueID;

		String issueSubject;
		String issueContent;
		String runTestTime;

		Node testCase;
		NodeList testCaseList;

		Element tesCaseElement;
		Issue issue;
		User issueAssignee;
		IssueManager issueManager;
		ProjectManager projectManager;
		DocumentBuilderFactory dbFactory;
		DocumentBuilder dBuilder;
		Document doc;
		File folderTest;

		issueManager = redmineMng.getIssueManager();
		projectManager = redmineMng.getProjectManager();

		try {
			dbFactory = DocumentBuilderFactory.newInstance();
			dBuilder = dbFactory.newDocumentBuilder();
			folderTest = new File(folderPath);
			for (File fXmlFile : folderTest.listFiles()) {

				if (TEST_RESULT_TYPE.equals(getExtension(fXmlFile.getName(), SYMBOL_DOT))) {

					doc = dBuilder.parse(fXmlFile);
					doc.getDocumentElement().normalize();
					runTestTime = getCurrentTime();
					testCaseList = doc.getElementsByTagName(TESTCASE);

					for (int temp = 0; temp < testCaseList.getLength(); temp++) {

						testCase = testCaseList.item(temp);
						tesCaseElement = (Element) testCase;
						issueSubject = tesCaseElement.getAttribute(TESTCASE_ATTRIBUTE_NAME);
						issueID = Integer.parseInt(getIssueID(issueSubject, SYMBOL_HAIFUN));
						issue = issueManager.getIssueById(issueID);
						issueAssignee = issue.getAssignee();

						if (testCase.hasChildNodes()) {

							issueContent = tesCaseElement.getTextContent();
							issueSubject = SYMBOL_PAREN_LEFT + FAILED + SYMBOL_PAREN_RIGHT + SYMBOL_PAREN_LEFT
									+ runTestTime + SYMBOL_PAREN_RIGHT + issueSubject;
							updateTestCaseIssue(issueManager, issueID, statusID, FAILED);
							createIssue(issueManager, projectManager, issueID, projectKey, issueSubject, issueContent,
									issueAssignee, trackerName, FAILED);

						} else {

							issueContent = EXECUTE_TIME + tesCaseElement.getAttribute(TESTCASE_ATTRIBUTE_TIME);
							issueSubject = SYMBOL_PAREN_LEFT + PASSED + SYMBOL_PAREN_RIGHT + SYMBOL_PAREN_LEFT
									+ runTestTime + SYMBOL_PAREN_RIGHT + issueSubject;
							updateTestCaseIssue(issueManager, issueID, statusID, PASSED);
							createIssue(issueManager, projectManager, issueID, projectKey, issueSubject, issueContent,
									issueAssignee, trackerName, PASSED);

						}
					}
				}
			}
		} catch (Exception e) {

			e.printStackTrace();

		}
	}

	/**
	 * This method is used to update status of issue
	 * 
	 * @param folderPath
	 * @param projectKey
	 */
	public void updateTestCaseIssue(IssueManager issueManager, Integer issueID, int statusID, String content) {
		try {

			Issue issue = issueManager.getIssueById(issueID);
			issue.getCustomFieldByName(UT_CUSTOMIZE_FIELD_TEST_RESULT).setValue(content);
			issue.setStatusId(statusID);
			issue.getRelations();
			issueManager.update(issue);

		} catch (RedmineException e) {

			e.printStackTrace();

		}
	}

	/**
	 * This method is used to update status of issue
	 * 
	 * @param issueManager
	 * @param projectManager
	 * @param relatedIssueID
	 * @param projectKey
	 * @param issueSubject
	 * @param issueContent
	 * @param assignee
	 * @param trackerName
	 */
	public void createIssue(IssueManager issueManager, ProjectManager projectManager, int relatedIssueID,
			String projectKey, String issueSubject, String issueContent, User assignee, String trackerName,
			String categoryName) {
		try {
			int issueID;
			Project projectByKey;
			Issue issue;
			Version ver;
			IssueCategory category;

			projectByKey = projectManager.getProjectByKey(projectKey);
			issue = IssueFactory.create(projectByKey.getId(), issueSubject);
			ver = VersionFactory.create(DEFAULT_VERSION);
			issue.setTargetVersion(ver);
			issue.setTracker(projectByKey.getTrackerByName(trackerName));
			category = getCategoryByName(categoryName, projectByKey);
			issue.setCategory(category);
			issue.setProject(projectByKey);
			issue.setDescription(issueContent);
			issue.setAssignee(assignee);
			issueID = redmineMng.getIssueManager().createIssue(issue).getId();
			createRelation(issueManager, issueID, relatedIssueID);

		} catch (RedmineException e) {

			e.printStackTrace();

		}
	}

	/**
	 * This method is to create relation beetwen UT Testcase & Bug
	 * 
	 * @param issueManager
	 * @param issueID
	 * @param relatedIssueID
	 */
	public void createRelation(IssueManager issueManager, int issueID, int relatedIssueID) {
		try {

			issueManager.createRelation(issueID, relatedIssueID, RELATION_TYPE);

		} catch (RedmineException e) {

			e.printStackTrace();

		}
	}

	/**
	 * This method is to get extension of file
	 * 
	 * @param filename
	 * @param extensionSymbol
	 */
	public String getExtension(String filename, String extensionSymbol) {

		if (filename == null) {

			return null;

		}
		int extensionPos = filename.lastIndexOf(extensionSymbol);
		return filename.substring(extensionPos + 1);
	}

	/**
	 * This method is to get current time
	 */
	public String getCurrentTime() {

		DateFormat dateFormat = new SimpleDateFormat(TIME_REGEX);
		Calendar cal = Calendar.getInstance();
		String timestamp = dateFormat.format(cal.getTime()).toString();
		return timestamp;

	}

	/**
	 * This method is to get issueID from XML UT Test Result
	 * 
	 * @param filename
	 * @param extensionSymbol
	 */
	public String getIssueID(String filename, String extensionSymbol) {

		if (filename == null) {
			return null;
		}
		return filename.split(extensionSymbol)[1];

	}

	/**
	 * This method is to get statusID by Name
	 * 
	 * @param statusName
	 */
	public int getStatusIdByName(String statusName) {
		try {

			List<IssueStatus> statusList = redmineMng.getIssueManager().getStatuses();
			for (Iterator<IssueStatus> iter = statusList.iterator(); iter.hasNext();) {

				IssueStatus issueStatus = iter.next();
				if (statusName.equals(issueStatus.getName())) {

					return issueStatus.getId();

				}
			}

		} catch (RedmineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * This method is get category by name
	 * 
	 * @param categoryName
	 * @param projectByKey
	 */
	public IssueCategory getCategoryByName(String categoryName, Project projectByKey) {
		try {

			List<IssueCategory> catgoryList = redmineMng.getIssueManager().getCategories(projectByKey.getId());
			for (Iterator<IssueCategory> iter = catgoryList.iterator(); iter.hasNext();) {

				IssueCategory issueCategory = iter.next();
				if (categoryName.equals(issueCategory.getName())) {
					return issueCategory;
				}
			}

		} catch (RedmineException e) {
			e.printStackTrace();
		}
		return null;
	}

}
