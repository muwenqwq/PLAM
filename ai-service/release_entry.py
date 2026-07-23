import argparse
import multiprocessing
import os
from pathlib import Path

import uvicorn

from app.main import app


def main() -> None:
    parser = argparse.ArgumentParser(description="EduAgent Studio AI service")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=18000)
    args = parser.parse_args()

    data_dir = Path(os.getenv("EDUAGENT_DATA_DIR", Path.cwd()))
    data_dir.mkdir(parents=True, exist_ok=True)
    os.chdir(data_dir)
    uvicorn.run(app, host=args.host, port=args.port, log_level="info", access_log=False)


if __name__ == "__main__":
    multiprocessing.freeze_support()
    main()
