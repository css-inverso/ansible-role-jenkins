import com.nirima.jenkins.plugins.docker.*
import hudson.slaves.Cloud
import io.jenkins.docker.client.DockerAPI
import io.jenkins.docker.connector.DockerComputerAttachConnector
import jenkins.model.Jenkins
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerEndpoint

import static java.util.stream.Collectors.toList

String dockerHostUrl = '{{ jenkins_docker_cloud_tcp_url }}'
String cloudName = '{{ jenkins_docker_cloud_name }}'
String dockerRegistry = '{{ jenkins_docker_registry }}'

/////////////////////////////////////////////////////////////////////////////
// Parameters in this script are placed in groovy maps.
// Most parameters are strings in 'single quotes'.
// Some are boolean, so either true or false.
// Some are numbers, either Integer or Long.
// Most parameters are optional.
// Default values are usually an empty string, false, or no number set.
/////////////////////////////////////////////////////////////////////////////

// Parameters listed here are used to create a
// https://github.com/jenkinsci/docker-plugin/blob/master/src/main/java/com/nirima/jenkins/plugins/docker/DockerTemplateBase.java
def templateBaseParameters = [
        // image is mandatory param: use 'jenkins/agent:latest' if unsure.
        image: 'jenkins/agent:latest',
        // all other parameters are optional
        // Uncomment them if you want to set them.
        // bindAllPorts:             false,
        // bindPorts:                '',
        // capabilitiesToAddString:  '',
        // capabilitiesToDropString: '',
        // cpuPeriod:                (Long)null,
        // cpuQuota:                 (Long)null,
        // cpuShares:                (Integer)null,
        // devicesString:            '',
        // dnsString:                '',
        // dockerCommand:            '',
        // environmentsString:       '',
        // extraDockerLabelsString:  '',
        // extraGroupsString:        '',
        // extraHostsString:         '',
        // hostname:                 '',
        // macAddress:               '',
        // memoryLimit:              (Integer)null,
        // memorySwap:               (Integer)null,
        // network:                  '',
        // privileged:               false,
        // pullCredentialsId:        '',
        // securityOptsString:       '',
        // shmSize:                  (Integer)null,
        tty  : true,
        // user:                     '',
        // volumesFromString:        '',
        // mountsString:             '',
]

// Parameters listed here are used to create a
// https://github.com/jenkinsci/docker-plugin/blob/master/src/main/java/com/nirima/jenkins/plugins/docker/DockerTemplate.java

def images = [
        [name: 'Java-Wölkchen', labels: 'linux java jdk-8 jdk-11 jdk-17 jdk-8-ibm', image: 'java-woelkchen:latest'],
        [name: 'Docker-Wölkchen', labels: 'linux docker', image: 'docker-woelkchen:latest'],
        [name: 'Node-Wölkchen', labels: 'linux nodejs yarn nodejs-14', image: 'node-woelkchen:latest'],
        [name: 'Node12-Wölkchen', labels: 'linux nodejs yarn nodejs-12', image: 'node-woelkchen:nodejs12'],
        [name: 'Sonar-Wölkchen', labels: 'linux sonar', image: 'sonar-woelkchen:latest'],
        [name: 'Tex-Wölkchen', labels: 'linux tex', image: 'tex-woelkchen:latest']
]

// Parameters listed here are used to create a
// https://github.com/jenkinsci/docker-plugin/blob/master/src/main/java/io/jenkins/docker/client/DockerAPI.java
// and
// https://github.com/jenkinsci/docker-plugin/blob/master/src/main/java/com/nirima/jenkins/plugins/docker/DockerCloud.java

def cloudParameters = [
        // serverUrl and name are required.
        // everything else is optional
        serverUrl     : dockerHostUrl,
        name          : cloudName,
        containerCap  : 0, // 0 means no cap
        // credentialsId:            '',
        connectTimeout: 60,
        readTimeout   : 60,
        // version:                  '',
        // dockerHostname:           '',
        // exposeDockerHost:         false,
        // disabled:                 false,
        // errorDuration:            (Integer)null,
]

/////////////////////////////////////////////////////////////////////////////
// The code above defines our data.
// Now to turn that raw data into objects used by the
// docker-plugin code...
/////////////////////////////////////////////////////////////////////////////

Set<String> templateParametersHandledSpecially = ['image', 'labelString', 'instanceCapStr']

private static List<DockerTemplate> getTemplates(List<LinkedHashMap<String, Serializable>> maps, templateBaseParameters, templateParametersHandledSpecially) {
    maps.stream().map {
        [
                instanceCapStr: 2,
                image         : dockerRegistry + '/' + it.image,
                labelString   : it.labels,
                mode          : hudson.model.Node.Mode.EXCLUSIVE,
                name          : it.name,
                pullTimeout   : 300,
                pullStrategy  : DockerImagePullStrategy.PULL_ALWAYS,
                removeVolumes : true
        ]
    }.map {
        DockerTemplateBase templateBase = new DockerTemplateBase(it.image as String)
        templateBaseParameters.findAll { it.key != "image" }.each { k, v ->
            templateBase."$k" = v
        }
        DockerTemplate template = new DockerTemplate(
                templateBase,
                new DockerComputerAttachConnector(),
                it.labelString as String,
                it.instanceCapStr as String
        )
        it.findAll { !templateParametersHandledSpecially.contains(it.key) }.each { k, v ->
            if (k == "disabled") {
                DockerDisabled dd = new DockerDisabled()
                dd.disabledByChoice = v
                template."$k" = dd
            } else {
                template."$k" = v
            }
        }
        return template
    }.collect(toList())
}

def templates = getTemplates(images, templateBaseParameters, templateParametersHandledSpecially)

// get Jenkins instance
Jenkins jenkins = Jenkins.getInstanceOrNull()

Set<String> cloudParametersHandledSpecially = ['serverUrl',
                                               'credentialsId',
                                               'credentialsId',
                                               'connectTimeout',
                                               'readTimeout',
                                               'version',
                                               'connectTimeout',
                                               'dockerHostname',
                                               'name']
[cloudParameters].forEach { map ->
    DockerAPI api = new DockerAPI(new DockerServerEndpoint(map.serverUrl as String, map.credentialsId as String))
    api.with {
        connectTimeout = map.connectTimeout as int
        readTimeout = map.readTimeout as int
        apiVersion = map.version
        hostname = map.dockerHostname
    }
    DockerCloud newCloud = new DockerCloud(
            map.name as String,
            api,
            templates
    )
    map.findAll { !cloudParametersHandledSpecially.contains(it.key) }.each { k, v ->
        if (k == "disabled") {
            DockerDisabled dd = new DockerDisabled()
            dd.disabledByChoice = v
            newCloud."$k" = dd
        } else {
            newCloud."$k" = v
        }
    }

/////////////////////////////////////////////////////////////////////////////
// Now to push our data into Jenkins,
// replacing (overwriting) any cloud of the same name with this config.
/////////////////////////////////////////////////////////////////////////////

// add/replace cloud configuration to Jenkins
    Cloud oldCloudOrNull = jenkins.clouds.getByName(map.name as String)
    if (oldCloudOrNull) {
        jenkins.clouds.remove(oldCloudOrNull)
    }
    jenkins.clouds.add(newCloud)
}
// save current Jenkins state to disk
jenkins.save()
