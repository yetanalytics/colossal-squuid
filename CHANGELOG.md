# Change Log

## 0.1.5
- Update GitHub CI and CD to remove deprecation warnings.

## 0.1.4

- Update README by adding an API section. ([#16](https://github.com/yetanalytics/colossal-squuid/pull/16))
- Update CD workflow such that Clojars deployment occurs in a separate GitHub Action. ([#17](https://github.com/yetanalytics/colossal-squuid/pull/17))
- Add changelog, contributing guidelines, and code of conduct. ([#19](https://github.com/yetanalytics/colossal-squuid/pull/19))
- Add the `uuid->time` API function to extract the timestamp from a SQUUID. ([#18](https://github.com/yetanalytics/colossal-squuid/pull/18))

## 0.1.3

- Fix bug from 0.1.2 where JAR file is not compiled with the correct files. ([#15](https://github.com/yetanalytics/colossal-squuid/pull/15))

## 0.1.2

- Incorporate automatic Clojars deployment in CD workflow. ([#14](https://github.com/yetanalytics/colossal-squuid/pull/14), with ideas incorporated from [#13](https://github.com/yetanalytics/colossal-squuid/pull/13))

## 0.1.1

- Fix [issue](https://github.com/yetanalytics/colossal-squuid/issues/10) where strict monotonicity was not enforced on multiple threads. ([#11](https://github.com/yetanalytics/colossal-squuid/pull/11))

## 0.1.0

Initial release!
