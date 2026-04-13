from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


ENV_PATH = Path(__file__).resolve().parents[2] / ".env"


@dataclass(frozen=True)
class SiteCredentials:
    provider: str
    username: str
    password: str

    @property
    def configured(self) -> bool:
        return bool(self.username and self.password)


def _parse_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        values[key] = value
    return values


def load_site_credentials(provider: str) -> SiteCredentials:
    upper_provider = provider.strip().upper()
    file_values = _parse_env_file(ENV_PATH)

    username = os.environ.get(f"{upper_provider}_ID", file_values.get(f"{upper_provider}_ID", ""))
    password = os.environ.get(
        f"{upper_provider}_PASSWORD",
        file_values.get(f"{upper_provider}_PASSWORD", ""),
    )

    return SiteCredentials(
        provider=upper_provider,
        username=username,
        password=password,
    )
