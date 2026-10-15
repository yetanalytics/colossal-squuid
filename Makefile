.PHONY: clean dependency-pom test-clj test-cljs ci

clean:
	rm -rf cljs-test-runner-out target

# Keep the publishing POM intact; generate a separate dependency submission POM.
dependency-pom:
	mkdir -p target/dependency-submission
	cd target/dependency-submission && clojure -X:deps mvn-pom :user nil :project '"../../deps.edn"'

test-clj:
	clojure -M:test:runner-clj

test-cljs:
	clojure -M:test:runner-cljs

ci: test-clj test-cljs

