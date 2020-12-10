.PHONY: build
build:
	# mvn clean generate-sources package
	mvn install
	( cd jplag && mvn clean generate-sources assembly:assembly)

.PHONY: clean
clean:
	mvn clean
	( cd jplag && mvn clean )
	- rm -r $(HOME)/.m2/repository/edu/
	find . -name 'target' -type d -exec rm -rf {} +
