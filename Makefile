.PHONY: clean test-clj test-cljs ci

clean:
	rm -rf cljs-test-runner-out target

test-clj:
	clojure -M:test:runner-clj

test-cljs:
	clojure -M:test:runner-cljs

ci: test-clj test-cljs

# Default
VERSION = LATEST

build:
	clojure -X:build jar :lib com.yetanalytics/colossal-squuid :version '"${VERSION}"' :src-dirs '["src/main"]'

deploy:
	clojure -X:build deploy :lib com.yetanalytics/colossal-squuid :version '"${VERSION}"'
