#!/usr/bin/env python3
"""
Performance Regression Detector

This script analyzes JMH benchmark results and detects performance regressions
by comparing current results with baseline results.
"""

import json
import os
import re
import sys
from typing import Dict, List, Optional, Tuple


def parse_benchmark_results(file_path: str) -> Dict[str, float]:
    """
    Parse JMH benchmark results from a text file.
    Returns a dictionary of benchmark names to scores (ops/sec).
    """
    results = {}

    if not os.path.exists(file_path):
        return results

    with open(file_path, 'r') as f:
        content = f.read()

    # Pattern to match JMH benchmark results
    # Example: "BenchmarkName  thrpt   25  123456.789 ops/sec"
    pattern = r'(\w+\.\w+)\s+\w+\s+\d+\s+(\d+\.?\d*)\s+ops/sec'
    matches = re.findall(pattern, content)

    for benchmark_name, score in matches:
        try:
            results[benchmark_name] = float(score)
        except ValueError:
            continue

    return results


def load_baseline_results(baseline_path: str) -> Dict[str, float]:
    """
    Load baseline benchmark results from a JSON file.
    """
    if not os.path.exists(baseline_path):
        return {}

    try:
        with open(baseline_path, 'r') as f:
            return json.load(f)
    except (json.JSONDecodeError, IOError):
        return {}


def save_baseline_results(results: Dict[str, float], baseline_path: str):
    """
    Save current results as new baseline.
    """
    os.makedirs(os.path.dirname(baseline_path), exist_ok=True)
    with open(baseline_path, 'w') as f:
        json.dump(results, f, indent=2)


def detect_regressions(
    current_results: Dict[str, float],
    baseline_results: Dict[str, float],
    threshold_percent: float = 10.0
) -> List[Tuple[str, float, float, float]]:
    """
    Detect performance regressions by comparing current results with baseline.

    Args:
        current_results: Current benchmark results
        baseline_results: Baseline benchmark results
        threshold_percent: Percentage threshold for regression detection

    Returns:
        List of tuples: (benchmark_name, current_score, baseline_score, regression_percent)
    """
    regressions = []

    for benchmark_name, baseline_score in baseline_results.items():
        if benchmark_name not in current_results:
            continue

        current_score = current_results[benchmark_name]

        # Calculate performance change percentage
        if baseline_score > 0:
            change_percent = ((current_score - baseline_score) / baseline_score) * 100

            # Detect regression (negative change beyond threshold)
            if change_percent < -threshold_percent:
                regressions.append((benchmark_name, current_score, baseline_score, change_percent))

    return regressions


def generate_regression_report(
    regressions: List[Tuple[str, float, float, float]],
    threshold_percent: float
) -> str:
    """
    Generate a human-readable regression report.
    """
    if not regressions:
        return "✅ No performance regressions detected."

    report = ["## ⚠️ Performance Regressions Detected\n"]
    report.append(f"Threshold: {threshold_percent}% performance degradation\n")

    for benchmark_name, current_score, baseline_score, regression_percent in regressions:
        report.append(f"### {benchmark_name}")
        report.append(f"- Current: {current_score:,.2f} ops/sec")
        report.append(f"- Baseline: {baseline_score:,.2f} ops/sec")
        report.append(f"- Regression: {regression_percent:.2f}%\n")

    return "\n".join(report)


def main():
    """
    Main function to run performance regression detection.
    """
    # Configuration
    benchmark_files = [
        "benchmark-results/middleware-benchmark.txt",
        "benchmark-results/security-benchmark.txt",
        "benchmark-results/database-benchmark.txt"
    ]
    baseline_file = "benchmark-baseline/baseline.json"
    threshold_percent = 10.0  # 10% performance degradation threshold

    # Parse current benchmark results
    current_results = {}
    for benchmark_file in benchmark_files:
        if os.path.exists(benchmark_file):
            file_results = parse_benchmark_results(benchmark_file)
            current_results.update(file_results)

    if not current_results:
        print("❌ No benchmark results found. Cannot perform regression detection.")
        sys.exit(1)

    # Load baseline results
    baseline_results = load_baseline_results(baseline_file)

    if not baseline_results:
        print("⚠️ No baseline results found. Saving current results as baseline.")
        save_baseline_results(current_results, baseline_file)
        print("✅ Current results saved as new baseline.")
        sys.exit(0)

    # Detect regressions
    regressions = detect_regressions(current_results, baseline_results, threshold_percent)

    # Generate report
    report = generate_regression_report(regressions, threshold_percent)
    print(report)

    # Save report to file
    with open("performance-regression-report.md", "w") as f:
        f.write(report)

    # Exit with error code if regressions detected
    if regressions:
        print(f"\n❌ {len(regressions)} performance regression(s) detected!")
        sys.exit(1)
    else:
        print("\n✅ Performance regression check passed!")

        # Update baseline if no regressions
        save_baseline_results(current_results, baseline_file)
        print("✅ Baseline updated with current results.")


if __name__ == "__main__":
    main()