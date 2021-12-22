# colossal-squuid

[![CI](https://github.com/yetanalytics/colossal-squuid/actions/workflows/main.yml/badge.svg)](https://github.com/yetanalytics/colossal-squuid/actions/workflows/main.yml)

Library for generating Sequential UUIDs, or SQUUIDs.

## Overview

A SQUUID is a Universally Unique Identifier, or UUID, whose value increases strictly monotonically over time. A SQUUID generated later will always have a higher value, both lexicographically and in terms of the underlying bits, than one generated earlier. This is in contrast to regular UUIDs (specifically version 4 UUIDs) that are completely random. However, it is also useful for generated SQUUIDs to maintain some degree of randomness to preserve uniqueness and reduce collision, rather than being completely one-to-one with a particular timestamp.

## Installation

If using deps.edn, add the following line to your `:deps` map:
```clojure
com.yetanalytics/colossal-squuid {:mvn/version "0.1.3"}
```
See the [Clojars page](https://clojars.org/com.yetanalytics/colossal-squuid) for how to install via Leiningen or other methods.

**Note:** By default colossal-squuid will bring in the Clojure and ClojureScript libraries as transitive dependencies. If you wish to exclude these from your project (e.g. because it is clj or cljs-only), you can use the `:exclusions` keyword (which works for both [deps.edn](https://simonrobson.net/2019/04/16/clojure-deps-with-exclusions.html) and [Leiningen](https://github.com/technomancy/leiningen/blob/master/sample.project.clj#L55)):
```clojure
:exclusions [org.clojure/clojure org.clojure/clojurescript]
```

## API

Three functions are provided in the `com.yetanalytics.squuid` namespace:
- `generate-squuid` generates a SQUUID based off of a random base UUID and a timestamp representing the current time.
- `generate-squuid*`, which returns a map containing the base UUID, the timestamp, and the SQUUID.
- `time->uuid*` takes a timestamp and creates a SQUUID with a fixed (not random)  base UUID portion.

```clojure
(generate-squuid)
;; => #uuid "017de28f-5801-8c62-9ce9-cef70883794a"

(generate-squuid*)
;; => {:timestamp #inst "2021-12-22T14:33:04.769000000-00:00"
;;     :base-uuid #uuid "85335e1f-9c1f-4c62-9ce9-cef70883794a"
;;     :squuid    #uuid "017de28f-5801-8c62-9ce9-cef70883794a"}

(time->uuid #inst "2021-12-22T14:33:04.769000000-00:00")
;; => #uuid "017de28f-5801-8fff-8fff-ffffffffffff"
```

## Implementation

Our solution is to generate SQUUIDs where the first 48 bits are timestamp-based, while the remaining 80 bits are derived from a v4 base UUID. Abiding by RFC 4122, there are 6 reserved bits in a v4 UUID: 4 for the version (set at `0100`) and 2 for the variant (set at `11`). This means that there are 74 remaining random bits, which allows for about 18.9 sextillion random segments.

The timestamp is coerced to millisecond resolution. Specifically, it is the number of milliseconds since the start of the UNIX epoch on January 1, 1970. Due to the 48 bit maximum on the timestamp, the latest time supported is August 2, 10889; any millisecond-resolution timestamp generated after this date will have more than 48 bits.

If two SQUUIDs are generated in the same milliseconds, then instead of using completely different base UUIDs, the earlier SQUUID will be incremented by 1 to create the later SQUUID, ensuring strict monotonicity. In the very unlikely case the SQUUID cannot be incremented, an exception will be thrown.

The generated SQUUIDs have a version number of 8, as that is the version suggested by the draft RFC on SQUUIDs (see "Background").

A graphical representation of the generated SQUUIDs is as follows:
```
|-timestamp-| |----base v4 UUID----|
xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx
```
Where `M` is the version (always set to `8`) and `N` is the variant (which, since only the two most significant bits are fixed, can range from `8` to `B`).

## Background

- The original approach of generating a 48-bit timestamp and merging it into a v4 UUID is taken from the Laravel PHP library's `orderedUuid` function: https://itnext.io/laravel-the-mysterious-ordered-uuid-29e7500b4f8
- The idea of incrementing the least significant bit on a timestamp collision is taken from the ULID specification: https://github.com/ulid/spec
- The draft RFC for v8 UUIDs: https://datatracker.ietf.org/doc/html/draft-peabody-dispatch-new-uuid-format

## License

Copyright © 2021 Yet Analytics, Inc.

colossal-squuid is licensed under the Apache License, Version 2.0. See [LICENSE](https://github.com/yetanalytics/colossal-squuid/blob/main/LICENSE) for the full license text
