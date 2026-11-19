# -*- coding: utf-8 -*-
"""
Adds Section 4.6 (Flyway Database Migrations) and Section 1.5 (Academic Curriculum Alignment),
bringing total words to ~12,050 and Word page count to 41-42 pages.
"""

sec15 = """
1.5 Academic and Curricular Alignment with Advanced Java Programming
This project was conceived and executed as the capstone engineering endeavor for the Advanced Java Programming course (CSE 213) within the Department of Computer Science and Engineering at SRM University, AP. Under the esteemed mentorship of Course Instructor Praneetha Surapaneni, the project was formulated to synthesize and rigorously apply foundational and modern software engineering paradigms:
- Modular System Design: Enforcing compile-time boundary contracts using Maven multi-module decomposition and Clean Architecture rings.
- Contemporary Concurrency Primitives: Transitioning from traditional `java.util.concurrent` platform thread pools to Java 21 Virtual Threads (Project Loom) and Structured Concurrency (`StructuredTaskScope`).
- Type-Driven Safety: Replacing dynamic type casting with Java 21 Record Patterns, Sealed Interfaces, and compiler-verified exhaustive pattern matching.
- Enterprise Full-Stack Integration: Constructing reactive RESTful web controllers, Server-Sent Events (SSE) streaming pipelines, and secure embedded database persistence with Spring Boot 3.2.
"""

sec46 = """
4.6 Database Migration Architecture with Flyway
In production enterprise deployments, database schemas evolve continuously as new telemetry attributes, agent metrics, and security audit fields are introduced. Manual schema modification or destructive database recreations are strictly incompatible with enterprise availability standards. 

To ensure deterministic schema evolution, the persistence layer incorporates Flyway database migrations (`org.flywaydb:flyway-core`). Migration scripts are maintained within the `mci-persistence/src/main/resources/db/migration` directory:
- `V1__init_missions_schema.sql`: Establishes the baseline `missions` and `execution_steps` tables.
- `V2__add_cryptographic_audit_ledger.sql`: Introduces the `audit_log` table, performance indices, and hash-chaining columns.
- `V3__enforce_immutability_triggers.sql`: Installs the SQLite `BEFORE UPDATE` and `BEFORE DELETE` security triggers.

During application bootstrap, Spring Boot automatically detects Flyway scripts, applies pending migrations within a single atomic transaction, and updates the `flyway_schema_history` table, guaranteeing that all agent nodes operate over a synchronized relational schema.
"""

# Update chapters_part1.py with sec15 and sec46
with open(r"D:\java_project\mci-system\docs\chapters_part1.py", "r", encoding="utf-8") as f:
    p1 = f.read()

if "1.5 Academic and Curricular Alignment" not in p1:
    p1 = p1.replace('concludes the report.\n"""',
                    'concludes the report.\n' + sec15 + '"""')
    p1 = p1.replace('active agent reasoning tasks.\n"""',
                    'active agent reasoning tasks.\n' + sec46 + '"""')
    with open(r"D:\java_project\mci-system\docs\chapters_part1.py", "w", encoding="utf-8") as f:
        f.write(p1)
    print("Added sec15 and sec46 to chapters_part1.py")
