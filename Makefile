.PHONY: jenkins plugin

jenkins:
	docker build -t cloudshare/jenkins .

plugin: cloudshare-jenkins/target/cloudshare-jenkins.jar
	cd cloudshare-jenkins; mvn package
	cp cloudshare-jenkins/target/cloudshare-jenkins.jar ~/cloudshare/jenkins_home/plugins/cloudshare-jenkins/WEB-INF/lib/
	curl -XPOST -u admin:38a7e3b9158d49319e255a4d984cd8a1 http://localhost:8080/restart
