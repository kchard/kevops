import com.gitblit.GitBlit
import com.gitblit.Keys
import com.gitblit.models.RepositoryModel
import com.gitblit.models.UserModel
import com.gitblit.utils.JGitUtils
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.ReceiveCommand
import org.eclipse.jgit.transport.ReceiveCommand.Result
import org.slf4j.Logger
import groovy.json.JsonSlurper

def branchName = null
if(commands.size() == 1 && commands.get(0).getRefName().startsWith('refs/heads/')) {
	branchName = commands.get(0).getRefName().replace('refs/heads/', '')
}

if(branchName == null) return true //No need to create job, continue hook chain}

def jenkinsUrl = gitblit.getString("groovy.jenkinsServer", "ERROR")
def jobName = "${repository.name}-${branchName}"

def jobUrl = jenkinsUrl + '/api/json'
def response = new URL(jobUrl).getContent().text
def json = new JsonSlurper().parseText(response)
def foundJob = false
json.jobs.each { job ->
    if(jobName == job.name) {
		foundJob = true
    }
}

if(foundJob) return true //No need to create the job, we already have it

def config = """
<?xml version='1.0' encoding='UTF-8'?>
<project>
  <actions/>
  <description></description>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <scm class="hudson.plugins.git.GitSCM" plugin="git@1.5.0">
    <configVersion>2</configVersion>
    <userRemoteConfigs>
      <hudson.plugins.git.UserRemoteConfig>
        <name></name>
        <refspec></refspec>
        <url>${url}/git/${repository.name}</url>
      </hudson.plugins.git.UserRemoteConfig>
    </userRemoteConfigs>
    <branches>
      <hudson.plugins.git.BranchSpec>
        <name>*/${branchName}</name>
      </hudson.plugins.git.BranchSpec>
    </branches>
    <disableSubmodules>false</disableSubmodules>
    <recursiveSubmodules>false</recursiveSubmodules>
    <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
    <authorOrCommitter>false</authorOrCommitter>
    <clean>false</clean>
    <wipeOutWorkspace>false</wipeOutWorkspace>
    <pruneBranches>false</pruneBranches>
    <remotePoll>false</remotePoll>
    <ignoreNotifyCommit>false</ignoreNotifyCommit>
    <useShallowClone>false</useShallowClone>
    <buildChooser class="hudson.plugins.git.util.DefaultBuildChooser"/>
    <gitTool>Default</gitTool>
    <browser class="hudson.plugins.git.browser.GitBlitRepositoryBrowser">
      <url>${url}</url>
      <projectName>${repository.name}</projectName>
    </browser>
    <submoduleCfg class="list"/>
    <relativeTargetDir></relativeTargetDir>
    <reference></reference>
    <excludedRegions></excludedRegions>
    <excludedUsers></excludedUsers>
	<gitConfigName></gitConfigName>
    <gitConfigEmail></gitConfigEmail>
    <skipTag>false</skipTag>
    <includedRegions></includedRegions>
    <scmName></scmName>
  </scm>
  <canRoam>true</canRoam>
  <disabled>false</disabled>
  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
  <triggers>
    <hudson.triggers.SCMTrigger>
      <spec></spec>
      <ignorePostCommitHooks>false</ignorePostCommitHooks>
    </hudson.triggers.SCMTrigger>
  </triggers>
  <concurrentBuild>false</concurrentBuild>
  <builders>
    <hudson.tasks.Shell>
      <command>echo "SET ME!!!: ex. gradle(w) clean check; mvn clean test; ant clean test; lein do clean, test"</command>
    </hudson.tasks.Shell>
  </builders>
  <publishers/>
  <buildWrappers/>
</project>
"""

def url = new URL(jenkinsUrl + "/createItem?name=${jobName}")
def connection = url.openConnection()
connection.setRequestMethod("POST")
connection.addRequestProperty("Content-Type", "application/xml")
connection.doOutput = true

def writer = new OutputStreamWriter(connection.outputStream)
writer.write(config.toString())
writer.flush()
writer.close()
connection.connect()

if(connection.responseCode == 200) {
	logger.info("Created job: ${jobName} for branch: ${branchName} in repo: ${repository.name}")
} else {
	logger.error(connection.content.text)
}
