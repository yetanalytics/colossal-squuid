.PHONY: clean test-clj test-cljs ci

clean:
	rm -rf cljs-test-runner-out target

test-clj:
	clojure -M:test:runner-clj

test-cljs:
	clojure -M:test:runner-cljs

ci: test-clj test-cljs

