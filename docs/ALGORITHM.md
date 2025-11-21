# Deterministic URL Shortening Algorithm

A practical guide to how Linkforge generates short codes predictably and reliably.

## TL;DR

- Same URL in same workspace → Always same short code
- Different workspaces → Different codes for same URL
- Collisions are extremely rare but handled automatically
- No random generation = predictable infrastructure

---

## The Problem with Random Short Codes

Most URL shorteners use random generation:

```
POST /shorten {"url": "https://example.com"}
→ Returns: "abc123" (random)

POST /shorten {"url": "https://example.com"}
→ Returns: "xyz789" (different random code!)
```

**Problems:**
1. Can't predict what code a URL will get
2. Creating the same link twice creates duplicates
3. Hard to clean up duplicates across systems
4. No way to check "does this URL already have a code?" without querying the database

---

## How Linkforge Works: Deterministic Hashing

Linkforge uses cryptographic hashing to generate codes from the URL itself:

```
URL → Canonicalize → Hash → Encode → Short Code
```

### Step 1: URL Canonicalization

Before hashing, URLs are normalized to handle variations:

**Input:** Various forms of the same URL
```
HTTP://Example.COM/path
http://example.com:80/path
http://example.com/path/
http://example.com/path?z=1&a=2
```

**Output:** Single canonical form
```
http://example.com/path?a=2&z=1
```

**Normalization Rules:**
1. Lowercase scheme and host
2. Remove default ports (`:80` for HTTP, `:443` for HTTPS)
3. Remove trailing slashes (except root `/`)
4. Sort query parameters alphabetically
5. Remove URL fragments (`#section`)

**Why This Matters:**

These URLs all produce the same short code:
- `HTTP://EXAMPLE.COM/page`
- `http://example.com/page`
- `http://example.com:80/page`
- `http://example.com/page/`

Without canonicalization, each would get a different code, creating duplicates.

### Step 2: Deterministic Hash Generation

We construct a unique input string and hash it:

```
Hash Input = Canonical URL + "|" + Workspace ID
Example: "http://example.com/page|123"
```

**Hash Algorithm:** SHA-256 (256-bit cryptographic hash)

```
SHA-256("http://example.com/page|123")
→ e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```

**Why SHA-256?**
- Deterministic: Same input always produces same output
- Collision-resistant: Extremely unlikely to get same hash for different inputs
- Fast: < 0.1ms to compute
- Standard: Available in every programming language

**Why Include Workspace ID?**

Workspace isolation means different workspaces can have different codes for the same URL:

```
Hash Input = "http://example.com|workspace_1"
→ Short Code: "abc123"

Hash Input = "http://example.com|workspace_2"
→ Short Code: "xyz789"
```

### Step 3: Base58 Encoding

We take the first 16 bytes of the hash and encode them as Base58:

**Base58 Alphabet (58 characters):**
```
123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz
```

**Excluded characters (to avoid confusion):**
- `0` (zero) - looks like O
- `O` (capital O) - looks like 0
- `I` (capital i) - looks like l
- `l` (lowercase L) - looks like I or 1

**Example:**
```
Hash bytes: [227, 176, 196, 66, 152, 252, 28, 20, 154, 251, 244, 200, 153, 111, 185, 36]
→ Convert to integer: 303073173681590370994635802672784359204
→ Encode Base58: "MaSgB7xKpQ"
```

**Short Code Length:** 10 characters (default)

**Why 10 characters?**
- 58^10 ≈ 4.3 × 10^17 possible codes
- Even with 1 million URLs per workspace, collision probability < 0.0001%
- Short enough for humans, long enough for safety

---

## Collision Handling

### What is a Collision?

A collision occurs when two different URLs hash to the same short code. This is extremely rare but must be handled.

**Example:**
```
URL A: "http://example.com/page1" → Hash → "abc123"
URL B: "http://example.com/page2" → Hash → "abc123" (collision!)
```

### How We Handle It: Retry with Salt

When a collision is detected, we add a retry salt and rehash:

```
Attempt 0: Hash("http://example.com/page2|workspace_id")
→ Collision detected

Attempt 1: Hash("http://example.com/page2|workspace_id|1")
→ New short code generated

Attempt 2: Hash("http://example.com/page2|workspace_id|2")
→ New short code generated

... (up to 10 attempts)
```

**Detection Logic:**
```
1. Generate short code
2. Check database: Does this code exist with a DIFFERENT URL?
   - No → Success! Save and return
   - Yes → Collision! Increment salt and retry
3. If all 10 attempts fail → Error (should never happen in practice)
```

### Collision Probability

With 10-character Base58 codes:

| URLs per Workspace | Collision Probability |
|-------------------|-----------------------|
| 1,000 | 0.000000000116% |
| 10,000 | 0.00000116% |
| 100,000 | 0.000116% |
| 1,000,000 | 0.0116% (1 in 8,621) |
| 10,000,000 | 1.16% (1 in 86) |

**Real-world impact:**
- Most workspaces have < 10,000 links → virtually zero collisions
- Even at 1M links, 99.99% of URLs get their code on first attempt
- Retry mechanism ensures deterministic resolution

---

## Example Walkthrough

Let's shorten a URL step-by-step:

**Input:**
- URL: `HTTP://Example.com:80/api/users?name=john&id=123`
- Workspace ID: `42`

**Step 1: Canonicalize**
```
1. Lowercase scheme: "http"
2. Lowercase host: "example.com"
3. Remove default port: ":80" removed
4. Path stays: "/api/users"
5. Sort query params: "id=123&name=john"
6. Result: "http://example.com/api/users?id=123&name=john"
```

**Step 2: Hash**
```
Hash Input: "http://example.com/api/users?id=123&name=john|42"
SHA-256 Hash: "a3f8c9d2..."
First 16 bytes: [163, 248, 201, 210, ...]
```

**Step 3: Encode**
```
Integer: 218574629384756283746...
Base58: "K7mPq2XwZv"
```

**Step 4: Check Collision**
```
Query: SELECT * FROM short_link
       WHERE workspace_id = 42
       AND short_code = 'K7mPq2XwZv'

Result: No rows found → No collision!
```

**Step 5: Save**
```sql
INSERT INTO short_link (
  workspace_id, short_code, original_url, normalized_url, created_at
) VALUES (
  42, 'K7mPq2XwZv',
  'HTTP://Example.com:80/api/users?name=john&id=123',
  'http://example.com/api/users?id=123&name=john',
  NOW()
)
```

**Result:**
```json
{
  "short_code": "K7mPq2XwZv",
  "short_url": "https://short.ly/K7mPq2XwZv",
  "original_url": "HTTP://Example.com:80/api/users?name=john&id=123"
}
```

---

## Idempotency: The Magic of Determinism

Because codes are deterministic, calling the API multiple times with the same URL returns the same code:

```bash
# First call
POST /api/v1/workspaces/42/links
{"original_url": "https://example.com/page"}

Response: {"short_code": "K7mPq2XwZv"}

# Second call (same URL)
POST /api/v1/workspaces/42/links
{"original_url": "https://example.com/page"}

Response: {"short_code": "K7mPq2XwZv"}  # Same code!
```

**Under the hood:**
1. URL canonicalized: `https://example.com/page`
2. Check database: Does `(workspace_id=42, normalized_url='https://example.com/page')` exist?
3. Yes → Return existing short code
4. No → Generate new code

**No duplicate links created. Ever.**

---

## Advantages Over Random Generation

### 1. Predictable Infrastructure

```bash
# Deploy script can pre-generate codes
CODE=$(curl -X POST /api/links -d '{"url":"https://app.com/v1.2.3"}')
echo "Release link: https://short.ly/$CODE"

# Redeploy same script later
CODE=$(curl -X POST /api/links -d '{"url":"https://app.com/v1.2.3"}')
echo "Release link: https://short.ly/$CODE"  # Same code!
```

### 2. Duplicate Prevention

```bash
# Marketing creates link
curl -X POST /api/links -d '{"url":"https://example.com/promo"}'
→ "abc123"

# Engineering creates same link (accidentally)
curl -X POST /api/links -d '{"url":"https://example.com/promo"}'
→ "abc123"  # Reuses existing code, no duplicate!
```

### 3. Workspace Isolation

```bash
# Company A
POST /workspaces/1/links {"url": "https://example.com"}
→ "abc123"

# Company B
POST /workspaces/2/links {"url": "https://example.com"}
→ "xyz789"  # Different code, no collision

# Both companies can use their codes without conflicts
```

### 4. Cache Efficiency

Because the same URL always maps to the same code, caching is simpler:

```
# Traditional random shortener
URL1 → "abc" (call 1)
URL1 → "def" (call 2, different!)
Cache can't help

# Linkforge deterministic
URL1 → "abc" (call 1)
URL1 → "abc" (call 2, cache hit!)
```

---

## Limitations

### 1. Codes Are Not Sequential

You can't predict what the next code will be:

```
/shorten → "MaSgB7xKpQ"
/shorten → "K7mPq2XwZv"
/shorten → "Xy9KmN2qWz"
```

This is by design—sequential codes are easier to guess and scrape.

### 2. URL Changes = New Code

If you change the URL even slightly, you get a new code:

```
"https://example.com/page" → "abc123"
"https://example.com/page?ref=twitter" → "xyz789"  # Different!
```

**Workaround:** Use custom codes if you need stable identifiers across URL changes.

### 3. Collision Retry Is Non-Negotiable

In the rare event of a collision, the retry mechanism adds latency (1-3 DB queries instead of 1). This is the price of determinism.

---

## Implementation Details

### Database Constraints

Two unique constraints enforce determinism and isolation:

```sql
-- Ensures same URL in same workspace = same code
CREATE UNIQUE INDEX idx_workspace_normalized_url
ON short_link(workspace_id, normalized_url);

-- Ensures short code is unique within workspace
CREATE UNIQUE INDEX idx_workspace_shortcode
ON short_link(workspace_id, short_code);
```

### Service Layer Logic

```java
public ShortLinkResponse createShortLink(Long workspaceId, String url) {
    // Step 1: Canonicalize
    String normalized = UrlCanonicalizer.canonicalize(url);

    // Step 2: Check if exists
    Optional<ShortLink> existing = repository
        .findByWorkspaceAndNormalizedUrl(workspaceId, normalized);
    if (existing.isPresent()) {
        return toResponse(existing.get());  // Deterministic reuse
    }

    // Step 3: Generate code with collision handling
    String code = generateUniqueCode(workspaceId, normalized);

    // Step 4: Save and return
    ShortLink link = repository.save(new ShortLink(workspaceId, code, url, normalized));
    return toResponse(link);
}

private String generateUniqueCode(Long workspaceId, String normalized) {
    for (int salt = 0; salt < 10; salt++) {
        String code = ShortCodeGenerator.generate(normalized, workspaceId, salt);

        Optional<ShortLink> collision = repository
            .findByWorkspaceAndShortCode(workspaceId, code);

        if (collision.isEmpty()) {
            return code;  // Success!
        }

        // Collision detected, retry with incremented salt
    }

    throw new IllegalStateException("Failed to generate unique code after 10 attempts");
}
```

---

## Testing the Algorithm

### Test Case 1: Idempotency

```bash
# Create link
CODE1=$(curl -X POST /api/v1/workspaces/1/links -d '{"original_url":"https://example.com"}' | jq -r '.short_code')

# Create same link again
CODE2=$(curl -X POST /api/v1/workspaces/1/links -d '{"original_url":"https://example.com"}' | jq -r '.short_code')

# Should be identical
echo $CODE1
echo $CODE2
[ "$CODE1" == "$CODE2" ] && echo "✓ Idempotent"
```

### Test Case 2: Workspace Isolation

```bash
# Same URL in different workspaces
CODE_WS1=$(curl -X POST /api/v1/workspaces/1/links -d '{"original_url":"https://example.com"}' | jq -r '.short_code')
CODE_WS2=$(curl -X POST /api/v1/workspaces/2/links -d '{"original_url":"https://example.com"}' | jq -r '.short_code')

# Should be different
[ "$CODE_WS1" != "$CODE_WS2" ] && echo "✓ Workspace isolated"
```

### Test Case 3: URL Normalization

```bash
# These should all produce the same code
curl -X POST /api/v1/workspaces/1/links -d '{"original_url":"HTTP://EXAMPLE.COM/page"}'
curl -X POST /api/v1/workspaces/1/links -d '{"original_url":"http://example.com/page"}'
curl -X POST /api/v1/workspaces/1/links -d '{"original_url":"http://example.com:80/page"}'
curl -X POST /api/v1/workspaces/1/links -d '{"original_url":"http://example.com/page/"}'

# All return same short_code
```

---

## FAQ

**Q: What happens if I change a single character in the URL?**

A: You get a completely different short code. Hash functions have the "avalanche effect"—small input changes produce drastically different outputs.

**Q: Can I predict what short code my URL will get?**

A: Yes, if you know the canonicalization rules and have access to the workspace ID. You can run the same hash locally. But it's not human-predictable.

**Q: What if I want a custom code instead of a generated one?**

A: Use the `custom_code` parameter in the API request. Custom codes bypass the deterministic algorithm entirely.

**Q: Can two different URLs ever get the same short code?**

A: Not within the same workspace. The unique constraint prevents it. Across different workspaces, yes—that's intentional (workspace isolation).

**Q: What happens if the hash collision retry fails 10 times?**

A: The API returns a 500 error. This should never happen in practice—the probability is astronomically low (< 1 in 10^15).

**Q: Why not use a shorter code (e.g., 6 characters)?**

A: Collision probability increases dramatically:
- 6 chars: 58^6 ≈ 56 billion codes → 1% collision at 750,000 URLs
- 10 chars: 58^10 ≈ 430 quintillion codes → 1% collision at 650 million URLs

We chose 10 for safety margin.

---

## Further Reading

- [Technical Specification (ALGORITHM_SPEC.md)](ALGORITHM_SPEC.md) - Full algorithm with pseudocode
- [Database Schema (DATABASE_SCHEMA.md)](DATABASE_SCHEMA.md) - How constraints enforce determinism
- [Architecture Guide (ARCHITECTURE.md)](ARCHITECTURE.md) - System-wide design decisions

---

**Last Updated:** 2025-11-18
**Document Version:** 1.0
