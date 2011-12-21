import org.eclipse.orion.internal.server.core.IOUtilities;
import org.eclipse.orion.server.core.resources.UniversalUniqueIdentifier;
import org.junit.Ignore;
	@Test
	public void testDiffPaths() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());

		String projectName = getMethodName();
		JSONObject project = createProjectOrLink(workspaceLocation, projectName, gitDir.toString());

		JSONObject testTxt = getChild(project, "test.txt");
		modifyFile(testTxt, "hi");

		JSONObject folder1 = getChild(project, "folder");
		JSONObject folderTxt = getChild(folder1, "folder.txt");
		modifyFile(folderTxt, "folder change");

		WebRequest request = getGetGitDiffRequest(project.getJSONObject(GitConstants.KEY_GIT).getString(GitConstants.KEY_DIFF) + "?parts=diff", new String[] {"folder/folder.txt"});
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		StringBuilder sb1 = new StringBuilder();
		sb1.append("diff --git a/folder/folder.txt b/folder/folder.txt").append("\n");
		sb1.append("index 0119635..95c4c65 100644").append("\n");
		sb1.append("--- a/folder/folder.txt").append("\n");
		sb1.append("+++ b/folder/folder.txt").append("\n");
		sb1.append("@@ -1 +1 @@").append("\n");
		sb1.append("-folder").append("\n");
		sb1.append("\\ No newline at end of file").append("\n");
		sb1.append("+folder change").append("\n");
		sb1.append("\\ No newline at end of file").append("\n");
		assertEquals(sb1.toString(), response.getText());

		request = getGetGitDiffRequest(project.getJSONObject(GitConstants.KEY_GIT).getString(GitConstants.KEY_DIFF) + "?parts=diff", new String[] {"test.txt"});
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		StringBuilder sb2 = new StringBuilder();
		sb2.append("diff --git a/test.txt b/test.txt").append("\n");
		sb2.append("index 30d74d2..32f95c0 100644").append("\n");
		sb2.append("--- a/test.txt").append("\n");
		sb2.append("+++ b/test.txt").append("\n");
		sb2.append("@@ -1 +1 @@").append("\n");
		sb2.append("-test").append("\n");
		sb2.append("\\ No newline at end of file").append("\n");
		sb2.append("+hi").append("\n");
		sb2.append("\\ No newline at end of file").append("\n");
		assertEquals(sb2.toString(), response.getText());

		request = getGetGitDiffRequest(project.getJSONObject(GitConstants.KEY_GIT).getString(GitConstants.KEY_DIFF) + "?parts=diff", new String[] {"folder/folder.txt", "test.txt"});
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		sb1.append(sb2);
		assertEquals(sb1.toString(), response.getText());
	}

		request = getPostGitDiffRequest(gitDiffUri + "/test.txt", Constants.HEAD, false);
	@Test
	public void testDiffApplyPatch_modifyFile() throws Exception {
		// clone: create
		URI workspaceLocation = createWorkspace(getMethodName());
		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), null);
		IPath clonePath = new Path("file").append(project.getString(ProtocolConstants.KEY_ID)).makeAbsolute();
		clone(clonePath);

		// get project metadata
		WebRequest request = getGetFilesRequest(project.getString(ProtocolConstants.KEY_CONTENT_LOCATION));
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		project = new JSONObject(response.getText());
		JSONObject gitSection = project.optJSONObject(GitConstants.KEY_GIT);
		assertNotNull(gitSection);

		gitSection = project.getJSONObject(GitConstants.KEY_GIT);
		String gitDiffUri = gitSection.getString(GitConstants.KEY_DIFF);

		StringBuilder sb = new StringBuilder();
		sb.append("diff --git a/test.txt b/test.txt").append("\n");
		sb.append("index 30d74d2..8013df8 100644").append("\n");
		sb.append("--- a/test.txt").append("\n");
		sb.append("+++ b/test.txt").append("\n");
		sb.append("@@ -1 +1 @@").append("\n");
		sb.append("-test").append("\n");
		sb.append("\\ No newline at end of file").append("\n");
		sb.append("+patched").append("\n");
		sb.append("\\ No newline at end of file").append("\n");

		/*JSONObject patchResult = */patch(gitDiffUri, sb.toString());
		//		assertEquals("Ok", patchResult.getString(GitConstants.KEY_RESULT));

		JSONObject testTxt = getChild(project, "test.txt");
		assertEquals("patched", getFileContent(testTxt));
		String gitStatusUri = gitSection.getString(GitConstants.KEY_STATUS);
		assertStatus(new StatusResult().setModifiedNames("test.txt").setModifiedContents("patched"), gitStatusUri);
	}

	// TODO
	@Ignore("not reported as a format error")
	@Test
	public void testDiffApplyPatch_modifyFileFormatError() throws Exception {
		// clone: create
		URI workspaceLocation = createWorkspace(getMethodName());
		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), null);
		IPath clonePath = new Path("file").append(project.getString(ProtocolConstants.KEY_ID)).makeAbsolute();
		clone(clonePath);

		// get project metadata
		WebRequest request = getGetFilesRequest(project.getString(ProtocolConstants.KEY_CONTENT_LOCATION));
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		project = new JSONObject(response.getText());
		JSONObject gitSection = project.optJSONObject(GitConstants.KEY_GIT);
		assertNotNull(gitSection);

		gitSection = project.getJSONObject(GitConstants.KEY_GIT);
		String gitDiffUri = gitSection.getString(GitConstants.KEY_DIFF);

		StringBuilder sb = new StringBuilder();
		sb.append("diff --git a/test.txt b/test.txt").append("\n");
		sb.append("malformed patch").append("\n");

		/*JSONObject patchResult =*/patch(gitDiffUri, sb.toString());
		//		assertNull(patchResult.optString(GitConstants.KEY_RESULT, null));
		//		assertNotNull(patchResult.getJSONArray("FormatErrors"));

		// nothing has changed
		JSONObject testTxt = getChild(project, "test.txt");
		assertEquals("test", getFileContent(testTxt));
		String gitStatusUri = gitSection.getString(GitConstants.KEY_STATUS);
		assertStatus(StatusResult.CLEAN, gitStatusUri);
	}

	@Test
	public void testDiffApplyPatch_modifyFileApplyError() throws Exception {
		// clone: create
		URI workspaceLocation = createWorkspace(getMethodName());
		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), null);
		IPath clonePath = new Path("file").append(project.getString(ProtocolConstants.KEY_ID)).makeAbsolute();
		clone(clonePath);

		// get project metadata
		WebRequest request = getGetFilesRequest(project.getString(ProtocolConstants.KEY_CONTENT_LOCATION));
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		project = new JSONObject(response.getText());
		JSONObject gitSection = project.optJSONObject(GitConstants.KEY_GIT);
		assertNotNull(gitSection);

		gitSection = project.getJSONObject(GitConstants.KEY_GIT);
		String gitDiffUri = gitSection.getString(GitConstants.KEY_DIFF);

		StringBuilder sb = new StringBuilder();
		sb.append("diff --git a/test.txt b/test.txt").append("\n");
		sb.append("index 30d74d2..8013df8 100644").append("\n");
		sb.append("--- a/test.txt").append("\n");
		sb.append("+++ b/test.txt").append("\n");
		sb.append("@@ -1 +1 @@").append("\n");
		sb.append("-xxx").append("\n");
		sb.append("\\ No newline at end of file").append("\n");
		sb.append("+patched").append("\n");
		sb.append("\\ No newline at end of file").append("\n");

		/*JSONObject patchResult =*/patch(gitDiffUri, sb.toString());
		//		assertNull(patchResult.optString(GitConstants.KEY_RESULT, null));
		//		assertNotNull(patchResult.getJSONArray("ApplyErrors"));

		// nothing has changed
		JSONObject testTxt = getChild(project, "test.txt");
		assertEquals("test", getFileContent(testTxt));
		String gitStatusUri = gitSection.getString(GitConstants.KEY_STATUS);
		assertStatus(StatusResult.CLEAN, gitStatusUri);
	}

	@Test
	public void testDiffApplyPatch_addFile() throws Exception {
		// clone: create
		URI workspaceLocation = createWorkspace(getMethodName());
		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), null);
		IPath clonePath = new Path("file").append(project.getString(ProtocolConstants.KEY_ID)).makeAbsolute();
		clone(clonePath);

		// get project metadata
		WebRequest request = getGetFilesRequest(project.getString(ProtocolConstants.KEY_CONTENT_LOCATION));
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		project = new JSONObject(response.getText());
		JSONObject gitSection = project.optJSONObject(GitConstants.KEY_GIT);
		assertNotNull(gitSection);

		gitSection = project.getJSONObject(GitConstants.KEY_GIT);
		String gitDiffUri = gitSection.getString(GitConstants.KEY_DIFF);

		StringBuilder sb = new StringBuilder();
		sb.append("diff --git a/new.txt b/new.txt").append("\n");
		sb.append("new file mode 100644").append("\n");
		sb.append("index 0000000..8013df8 100644").append("\n");
		sb.append("--- /dev/null").append("\n");
		sb.append("+++ b/new.txt").append("\n");
		sb.append("@@ -0,0 +1 @@").append("\n");
		sb.append("+newborn").append("\n");
		sb.append("\\ No newline at end of file").append("\n");

		/*JSONObject patchResult = */patch(gitDiffUri, sb.toString());
		//		assertEquals("Ok", patchResult.getString(GitConstants.KEY_RESULT));

		JSONObject newTxt = getChild(project, "new.txt");
		assertEquals("newborn", getFileContent(newTxt));
		String gitStatusUri = gitSection.getString(GitConstants.KEY_STATUS);
		assertStatus(new StatusResult().setUntrackedNames("new.txt"), gitStatusUri);
	}

	@Test
	public void testDiffApplyPatch_deleteFile() throws Exception {
		// clone: create
		URI workspaceLocation = createWorkspace(getMethodName());
		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), null);
		IPath clonePath = new Path("file").append(project.getString(ProtocolConstants.KEY_ID)).makeAbsolute();
		clone(clonePath);

		// get project metadata
		WebRequest request = getGetFilesRequest(project.getString(ProtocolConstants.KEY_CONTENT_LOCATION));
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		project = new JSONObject(response.getText());
		JSONObject gitSection = project.optJSONObject(GitConstants.KEY_GIT);
		assertNotNull(gitSection);

		gitSection = project.getJSONObject(GitConstants.KEY_GIT);
		String gitDiffUri = gitSection.getString(GitConstants.KEY_DIFF);

		StringBuilder sb = new StringBuilder();
		sb.append("diff --git a/test.txt b/test.txt").append("\n");
		sb.append("deleted file mode 100644").append("\n");
		sb.append("index 8013df8..0000000 100644").append("\n");
		sb.append("--- a/test.txt").append("\n");
		sb.append("+++ /dev/null").append("\n");
		sb.append("@@ -1 +0,0 @@").append("\n");
		sb.append("-test").append("\n");

		/*JSONObject patchResult =*/patch(gitDiffUri, sb.toString());
		//		assertEquals("Ok", patchResult.getString(GitConstants.KEY_RESULT));

		String gitStatusUri = gitSection.getString(GitConstants.KEY_STATUS);
		assertStatus(new StatusResult().setMissingNames("test.txt"), gitStatusUri);
	}

		return getGetGitDiffRequest(location, new String[] {});
	}

	static WebRequest getGetGitDiffRequest(String location, String[] paths) {
		for (String path : paths) {
			requestURI = addParam(requestURI, "Path=" + path);
		}
	private static String addParam(String location, String param) {
		location += location.indexOf("?") != -1 ? "&" : "?";
		location += param;
		return location;
	}

	private static void patch(final String gitDiffUri, String patch) throws IOException, SAXException, JSONException {
		WebRequest request = getPostGitDiffRequest(gitDiffUri, patch, true);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_CREATED, response.getResponseCode());
		//		return new JSONObject(response.getText());
	}

	private static final String EOL = "\r\n"; //$NON-NLS-1$

	private static WebRequest getPostGitDiffRequest(String location, String str, boolean patch) throws JSONException, UnsupportedEncodingException {
		String boundary = new UniversalUniqueIdentifier().toBase64String();
		if (!patch) {
			JSONObject body = new JSONObject();
			body.put(GitConstants.KEY_COMMIT_NEW, str);
			str = body.toString();
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("--" + boundary + EOL);
			sb.append(ProtocolConstants.HEADER_CONTENT_TYPE + ": plain/text" + EOL + EOL); //$NON-NLS-1$
			sb.append(str);
			sb.append(EOL);
			// see GitDiffHandlerV1.readPatch(ServletInputStream, String)
			sb.append(EOL + "--" + boundary + "--" + EOL);
			str = sb.toString();
		}

		WebRequest request = new PostMethodWebRequest(requestURI, IOUtilities.toInputStream(str), "UTF-8");
		if (patch) {
			request.setHeaderField(ProtocolConstants.HEADER_CONTENT_TYPE, "multipart/related; boundary=\"" + boundary + '"'); //$NON-NLS-1$
		}
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals("--" + boundary)) {
					line = reader.readLine(); // Content-Type:{...}
					if (buf.length() > 0) {
						parts.add(buf.toString());
						buf.setLength(0);
					}
				} else {
					if (buf.length() > 0)
						buf.append("\n");
					buf.append(line);
		} finally {
			IOUtilities.safeClose(reader);