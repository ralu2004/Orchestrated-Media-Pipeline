import sys


def main() -> int:
    if len(sys.argv) < 3:
        print("Usage: transcribe.py <audio_path> <output_txt>", file=sys.stderr)
        return 2

    audio_path = sys.argv[1]
    output_txt = sys.argv[2]

    try:
        from faster_whisper import WhisperModel
    except Exception as exc:
        print(f"Failed to import faster_whisper: {exc}", file=sys.stderr)
        return 1

    try:
        model_name = "base"
        model = WhisperModel(model_name, compute_type="int8")
        segments, _ = model.transcribe(audio_path)
        transcript = " ".join(segment.text.strip() for segment in segments).strip()
        with open(output_txt, "w", encoding="utf-8") as f:
            f.write(transcript if transcript else "[empty transcript]")
    except Exception as exc:
        print(f"Transcription failed: {exc}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

