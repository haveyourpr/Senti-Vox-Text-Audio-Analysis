# api_v2 - Java port of the context-free TypeScript sentiment code

No Maven/Gradle, no external libraries (needs JDK 11+).

    cd api_v2
    javac Main.java
    java Main                    # runs built-in test cases, prints one JSON document (UTF-8)
    java Main "some comment" "another"   # analyses the given texts as a batch
    java -Ddataset.dir=/other/dataset Main   # optional: custom dataset folder

Datasets are loaded at runtime from `dataset/cf-data.json` and `dataset/stopwords.ts`
(the `123n, 456n, ...` BigInt literals are parsed straight out of the .ts file).
1.45M entries load in about half a second.

| TypeScript / Python           | Java                  |
|-------------------------------|-----------------------|
| cf-analyze.ts                 | SentimentAnalyzer.java|
| data-loader.ts + stopwords.ts | DataLoader.java, HashTable128.java |
| hash.ts (xxh3-ts XXH3_128)    | Xxh3.java (from scratch, seed 0, UTF-8 bytes) |
| min-heap.ts                   | MinHeap.java          |
| word-cloud.ts                 | WordCloud.java        |
| tokenizer.py                  | Tokenizer.java (+ Token.java) |
| utils/keys.ts (JSON_KEYS)     | JsonKeys.java  <-- ASSUMED values, see below |

## Things to check
1. `utils/keys.ts` was not in the zip. JsonKeys.java uses snake_case names
   (positive_score, negative_score, polarity_score, sentiment, positive_words,
   negative_words, unidentified_words, keywords, word_cloud). Edit that one file if yours differ.
2. Tokens get tags: digit runs -> "number", letter runs -> "word", everything else -> "punctuation".
   Only "word" tokens are analysed (same as `tag === "word"` in cf-analyze.ts).
3. The sample dataset only has 16 neutral entries, so the built-in tests show mostly
   unidentified words. With the full dataset, positive/negative words appear.

## How it was verified
* Xxh3.java vs the python `xxhash` library: 936 random inputs, lengths 0..5000 (all paths), 0 mismatches.
* 35 comments (English, Hindi, URLs/emails, emoji, empty, random) through Java vs the original
  TypeScript logic (real min-heap.ts, word-cloud.ts, xxh3-ts) fed with tokens from tokenizer.py:
  output byte-identical, including keyword order and the SVG word cloud.
* Tokenizer output identical to tokenizer.py on the same inputs.
