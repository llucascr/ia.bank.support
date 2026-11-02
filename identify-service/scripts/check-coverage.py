#!/usr/bin/env python3
"""Require more than 70% line coverage across all production code."""

import argparse
import os
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "report",
        nargs="?",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "target/site/jacoco/jacoco.xml",
    )
    report = parser.parse_args().report
    try:
        counter = ET.parse(report).getroot().find("./counter[@type='LINE']")
        if counter is None:
            raise ValueError("The report has no global line counter.")
        covered = int(counter.attrib["covered"])
        missed = int(counter.attrib["missed"])
        total = covered + missed
        if covered < 0 or missed < 0 or total == 0:
            raise ValueError("The report has no valid production code lines.")
    except (OSError, ET.ParseError, KeyError, ValueError) as error:
        print(f"::error::Unable to verify coverage: {error}", file=sys.stderr)
        return 1

    message = f"Line coverage: {100 * covered / total:.4f}% ({covered}/{total}). Required: >70%."
    print(message)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as output:
            output.write(message + "\n")

    # Compare integers so exactly 70% fails without rounding the measurement.
    if covered * 100 <= total * 70:
        print("::error::Line coverage must be greater than 70%.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
