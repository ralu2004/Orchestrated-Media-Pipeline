import pathlib
import sys


def _chunks(text: str, size: int = 4000):
    start = 0
    while start < len(text):
        yield text[start:start + size]
        start += size


def main() -> int:
    if len(sys.argv) < 4:
        print("Usage: translate.py <input_transcript> <output_translation> <lang>", file=sys.stderr)
        return 2

    input_transcript = pathlib.Path(sys.argv[1])
    output_translation = pathlib.Path(sys.argv[2])
    lang = sys.argv[3]

    try:
        from deep_translator import GoogleTranslator
    except Exception as exc:
        print(
            "Failed to import deep_translator. Install with: python -m pip install deep-translator. "
            f"Details: {exc}",
            file=sys.stderr,
        )
        return 1

    try:
        text = input_transcript.read_text(encoding="utf-8")
        output_translation.parent.mkdir(parents=True, exist_ok=True)

        translator = GoogleTranslator(source="auto", target=lang)
        translated_parts = []
        for piece in _chunks(text):
            if piece.strip():
                translated_parts.append(translator.translate(piece))
            else:
                translated_parts.append(piece)

        output_translation.write_text("".join(translated_parts), encoding="utf-8")
    except Exception as exc:
        print(f"Translation failed: {exc}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

