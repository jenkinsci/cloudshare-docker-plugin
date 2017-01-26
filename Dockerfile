FROM jenkins

ADD cloudshare-jenkins/target/cloudshare-jenkins.hpi $JENKINS_HOME/plugins/cloudshare-jenkins.hpi

