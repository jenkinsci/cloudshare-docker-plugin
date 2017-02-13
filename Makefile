.PHONY: build release

build:
	mvn package

release: build
	mvn release:prepare release:perform
