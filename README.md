# colossal-squuid

[![CI](https://github.com/yetanalytics/colossal-squuid/actions/workflows/main.yml/badge.svg)](https://github.com/yetanalytics/colossal-squuid/actions/workflows/main.yml)

Library for generating Sequential UUIDs, or SQUUIDs.

## Overview

A SQUUID is a Universally Unique Identifier, or UUID, whose value increases strictly monotonically over time. A SQUUID generated later will always have a higher value, both lexicographically and in terms of the underlying bits, than one generated earlier. This is in contrast to regular UUIDs (specifically version 4 UUIDs) that are completely random. However, it is also useful for generated SQUUIDs to maintain some degree of randomness to preserve uniqueness and reduce collision, rather than being completely one-to-one with a particular timestamp.

## Implementation

Our solution is to generate SQUUIDs where the first 48 bits are timestamp-based, while the remaining 80 bits are derived from a v4 base UUID. Abiding by RFC 4122, there are 6 reserved bits in a v4 UUID: 4 for the version (set at `0100`) and 2 for the variant (set at `11`). This means that there are 76 remaining random bits, which allows for about 18.9 sextillion random segments.

The timestamp is coerced to millisecond resolution. Specifically, it is the number of milliseconds since the start of the UNIX epoch on January 1, 1970. Due to the 48 bit maximum on the timestamp, the latest time supported is August 2, 10889; any millisecond-resolution timestamp generated after this date will have more than 48 bits.

If two SQUUIDs are generated in the same milliseconds, then instead of using completely different base UUIDs, the earlier SQUUID will be incremented by 1 to create the later SQUUID, ensuring strict monotonicity. In the very unlikely case the SQUUID cannot be incremented, an exception will be thrown.

The generated SQUUIDs have a version number of 8, as that is the version suggested by the draft RFC on SQUUIDs (see "Background").

A graphical representation of the generated SQUUIDs is as follows:
```
|-timestamp-| |----base v4 UUID----|
xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx
```
Where `M` is the version (always set to `8`) and `N` is the variant (which, since only the two most significant bits are fixed, can range from `8` to `B`).

Two functions are provided in the core API: `generate-squuid`, which generates a SQUUID, and `generate-squuid*`, which returns a map containing the base UUID and the timestamp, as well as the SQUUID. In addition, `time->uuid` is provided to create a SQUUID from a timestamp with a fixed base UUID portion.

## Background

- The original approach of generating a 48-bit timestamp and merging it into a v4 UUID is taken from the Laravel PHP library's `orderedUuid` function: https://itnext.io/laravel-the-mysterious-ordered-uuid-29e7500b4f8
- The idea of incrementing the least significant bit on a timestamp collision is taken from the ULID specification: https://github.com/ulid/spec
- The draft RFC for v8 UUIDs: https://datatracker.ietf.org/doc/html/draft-peabody-dispatch-new-uuid-format

## License

Copyright © 2021 Yet Analytics

colossal-squuid is licensed under the Apache License, Version 2.0. See [LICENSE](https://github.com/yetanalytics/colossal-squuid/blob/main/LICENSE) for the full license text

