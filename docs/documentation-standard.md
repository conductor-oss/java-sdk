# Documentation standard

Every primary guide must include:

- Audience and prerequisites.
- An explicit OSS/Orkes capability label when behavior differs.
- A security note when credentials, user data, tools, or external side effects are involved.
- Runnable commands or a clear **Fragment** label linked to a complete example/reference.
- Expected result, common failure modes, cleanup where resources are started, and next-step links.

Keep examples repository-native and use `<VERSION>` plus Maven Central rather than stale version literals. Generated Javadocs are the signature source of truth. CI validates internal links, curated paths, and representative compile/smoke-test paths.
