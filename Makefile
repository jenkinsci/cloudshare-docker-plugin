.PHONY: build release findbugs

build:
	mvn package

release: build
	mvn -e release:prepare release:perform

findbugs: build
	mvn findbugs:gui

