import pathlib
import subprocess
import sys
import tempfile


def main() -> int:
    if len(sys.argv) < 4:
        print("Usage: dub.py <input_translation> <output_audio_path> <lang>", file=sys.stderr)
        return 2

    input_translation = pathlib.Path(sys.argv[1])
    output_audio = pathlib.Path(sys.argv[2])
    lang = sys.argv[3]

    try:
        from gtts import gTTS
    except Exception as exc:
        print(
            "Failed to import gTTS. Install with: python -m pip install gTTS. "
            f"Details: {exc}",
            file=sys.stderr,
        )
        return 1

    try:
        text = input_translation.read_text(encoding="utf-8")
        output_audio.parent.mkdir(parents=True, exist_ok=True)

        with tempfile.NamedTemporaryFile(suffix=".mp3", delete=False) as tmp:
            tmp_mp3 = pathlib.Path(tmp.name)

        try:
            tts = gTTS(text=text if text.strip() else "Empty translation", lang=lang)
            tts.save(str(tmp_mp3))

            result = subprocess.run(
                [
                    "ffmpeg",
                    "-y",
                    "-i",
                    str(tmp_mp3),
                    "-c:a",
                    "aac",
                    str(output_audio),
                ],
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                check=False,
            )
            if result.returncode != 0:
                raise RuntimeError(f"ffmpeg conversion failed: {result.stdout}")
        finally:
            try:
                tmp_mp3.unlink(missing_ok=True)
            except Exception:
                pass
    except Exception as exc:
        print(f"Dubbing failed: {exc}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

