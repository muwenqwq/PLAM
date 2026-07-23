import re
from typing import Any


RESOURCE_TYPE_LABELS = {
    "lecture_note": "课程笔记",
    "summary": "知识点总结",
    "knowledge_graph": "思维导图",
    "quiz_set": "练习题",
    "review_outline": "复习提纲",
    "mistake_review": "错题整理",
    "plan": "学习计划",
    "case_task": "案例任务",
}


RESOURCE_TYPE_CONTRACTS: dict[str, dict[str, Any]] = {
    "lecture_note": {
        "purpose": "形成可以直接阅读和复习的完整讲义，不是只列知识点。",
        "required_sections": ["学习目标", "前置检查", "核心讲解", "例题或场景", "易错提醒", "即时自测", "复习清单"],
        "rules": ["逐个解释概念、适用条件、步骤和边界", "每个重点至少给一个具体例子", "用自测题检查是否真正理解"],
    },
    "summary": {
        "purpose": "把内容压缩成考前可快速回看的高密度总结。",
        "required_sections": ["一分钟速览", "核心结论", "概念关系", "容易混淆", "记忆线索", "自检清单"],
        "rules": ["使用对照表或清单", "结论必须具体到输入知识点", "突出区别、条件和常见误区"],
    },
    "knowledge_graph": {
        "purpose": "用 Mermaid 图展示概念之间的真实关系，并解释阅读顺序。",
        "required_sections": ["mermaid", "图谱说明", "关系解读", "学习顺序"],
        "rules": ["必须输出可渲染的 graph TD Mermaid", "节点使用短标签", "边要体现前置、组成、对比或应用关系"],
    },
    "quiz_set": {
        "purpose": "生成可直接作答和订正的四选一练习，不生成简答题。",
        "required_sections": ["练习说明", "选择题", "参考答案", "逐项解析", "复习建议"],
        "rules": ["每题恰好 A/B/C/D 四个选项", "答案只能是 A/B/C/D", "逐项说明每个选项正确或错误的原因"],
    },
    "review_outline": {
        "purpose": "形成按优先级和章节组织的复习框架，明确复习到什么程度。",
        "required_sections": ["复习范围", "优先级", "章节提纲", "必须会", "复习顺序", "考前检查"],
        "rules": ["每个章节包含概念、方法、典型题和验收标准", "区分必须掌握与了解内容", "给出可勾选检查项"],
    },
    "mistake_review": {
        "purpose": "基于真实错题或薄弱点形成可订正、可复测的错题档案。",
        "required_sections": ["错因诊断", "订正步骤", "正确规则", "变式练习", "复测标准"],
        "rules": ["没有真实错题时必须明确是待填写的订正模板", "不得虚构学生做错的题", "区分知识、审题、步骤和粗心等错因"],
    },
    "plan": {
        "purpose": "形成按天可执行的学习安排，而不是重复罗列学习知识点。",
        "required_sections": ["目标拆解", "每日安排", "学习任务", "学习产出", "验收标准", "调整规则"],
        "rules": ["每天有不同目标和任务", "每项任务包含时长、产出和完成标准", "安排诊断、学习、练习、复盘和最终检查"],
    },
    "case_task": {
        "purpose": "生成真实场景下可完成、可提交、可评价的综合案例任务。",
        "required_sections": ["案例背景", "任务目标", "任务要求", "交付物", "实施步骤", "评价标准", "提示"],
        "rules": ["场景必须与学科和知识点直接相关", "交付物可检查", "评价量规包含明确分值或等级"],
    },
}


def resource_contract(resource_type: str) -> dict[str, Any]:
    normalized = resource_type if resource_type in RESOURCE_TYPE_CONTRACTS else "case_task"
    return {
        "resource_type": normalized,
        "label": RESOURCE_TYPE_LABELS[normalized],
        **RESOURCE_TYPE_CONTRACTS[normalized],
    }


def build_resource_markdown(
    resource_type: str,
    title: str,
    subject: str,
    points: list[str],
    input_params: dict[str, Any] | None = None,
    profile_note: str = "",
    role_note: str = "",
) -> tuple[str, dict[str, Any], str, float]:
    params = input_params or {}
    normalized = resource_type if resource_type in RESOURCE_TYPE_CONTRACTS else "case_task"
    clean_points = [point.strip() for point in points if point and point.strip()] or [subject]
    context = _context_entries(params)
    renderers = {
        "lecture_note": _lecture_note,
        "summary": _summary,
        "knowledge_graph": _knowledge_graph,
        "quiz_set": _quiz_set,
        "review_outline": _review_outline,
        "mistake_review": _mistake_review,
        "plan": _plan,
        "case_task": _case_task,
    }
    body, extra = renderers[normalized](title, subject, clean_points, context, params)
    appendix = _context_appendix(context)
    personalization = role_note + profile_note
    content = body + personalization + appendix
    checks = validate_resource_markdown(normalized, content)
    content_json = {
        "resource_type": normalized,
        "contract_version": "2026-07-type-contract-v1",
        "required_sections": RESOURCE_TYPE_CONTRACTS[normalized]["required_sections"],
        "quality_checks": checks,
        "source_files": list(dict.fromkeys(item[0] for item in context)),
        **extra,
    }
    label = RESOURCE_TYPE_LABELS[normalized]
    summary = f"已按{label}的专属结构生成，包含 {len(clean_points)} 个重点内容。"
    score = 92.0 if checks["valid"] else 82.0
    return content, content_json, summary, score


def validate_resource_markdown(resource_type: str, content: str) -> dict[str, Any]:
    contract = resource_contract(resource_type)
    required = contract["required_sections"]
    missing = [marker for marker in required if marker.lower() not in content.lower()]
    minimum = 300 if resource_type == "mistake_review" else 450 if resource_type == "knowledge_graph" else 650 if resource_type in {"summary", "case_task"} else 900
    return {
        "valid": len(content.strip()) >= minimum and not missing,
        "character_count": len(content.strip()),
        "minimum_character_count": minimum,
        "missing_sections": missing,
    }


def _lecture_note(title: str, subject: str, points: list[str], context: list[tuple[str, str]], _: dict[str, Any]) -> tuple[str, dict[str, Any]]:
    sections = []
    for index, point in enumerate(points, start=1):
        evidence = _evidence_for(index - 1, context)
        sections.append(
            f"### {index}. {point}\n\n"
            f"**概念定位**：{point} 是本次 {subject} 学习中的第 {index} 个重点。先回答‘它解决什么问题’，再掌握适用条件、操作过程和边界。\n\n"
            f"**核心讲解**\n\n"
            f"1. 定义：用自己的话给 {point} 下定义，并圈出关键词。\n"
            f"2. 条件：列出使用 {point} 前必须满足的前提。\n"
            f"3. 过程：按输入、处理、输出三个环节复述其工作过程。\n"
            f"4. 边界：说明它不适用的情况，以及容易与哪些概念混淆。\n\n"
            f"**资料依据**：{evidence}\n\n"
            f"**例题或场景**：在一个具体的 {subject} 问题中指出何时需要 {point}，写出判断依据和完整处理步骤。\n\n"
            f"**易错提醒**：只背结论、忽略适用条件，或无法解释步骤之间的因果关系，都不算真正掌握。\n\n"
            f"**即时自测**\n\n"
            f"- 不看资料，用 60 秒解释 {point}。\n"
            f"- 给出一个适用例子和一个不适用例子。\n"
            f"- 写出一道题中识别 {point} 的关键词。"
        )
    body = (
        f"# {title}\n\n> 资源定位：这是一份可直接阅读、练习和复盘的课程笔记。\n\n"
        f"## 学习目标\n\n- 理解 {subject} 中各重点的作用与联系。\n- 能独立解释条件和步骤。\n- 能通过例题验证理解。\n\n"
        "## 前置检查\n\n开始前写下：我已经会什么、最不确定什么、完成后要能做什么。\n\n"
        "## 核心讲解\n\n" + "\n\n".join(sections) +
        "\n\n## 复习清单\n\n- [ ] 每个概念能用一句话定义。\n- [ ] 每个重点能给出适用条件和反例。\n- [ ] 每个步骤能解释为什么这样做。\n- [ ] 完成即时自测并记录不会的部分。"
    )
    return body, {"section_count": len(points), "format": "guided_lecture_note"}


def _summary(title: str, subject: str, points: list[str], context: list[tuple[str, str]], _: dict[str, Any]) -> tuple[str, dict[str, Any]]:
    rows = []
    conclusions = []
    confusions = []
    for index, point in enumerate(points, start=1):
        rows.append(f"| {point} | 作用、条件、步骤、边界 | 能解释并用于一道题 | {_short_evidence(index - 1, context)} |")
        conclusions.append(f"{index}. **{point}**：掌握标准不是记住名称，而是能说清作用、条件并完成一次应用。")
        other = points[index % len(points)] if len(points) > 1 else f"{point} 的相近概念"
        confusions.append(f"- **{point} vs {other}**：从目标、适用条件、处理过程和结果四个维度比较。")
    body = (
        f"# {title}\n\n## 一分钟速览\n\n"
        "| 核心点 | 必须抓住 | 掌握证据 | 资料线索 |\n|---|---|---|---|\n" + "\n".join(rows) +
        "\n\n## 核心结论\n\n" + "\n".join(conclusions) +
        "\n\n## 概念关系\n\n学习顺序建议：先明确问题目标，再判断适用条件，然后执行步骤，最后用结果反查边界。各知识点应放到同一问题链中理解。\n\n"
        "## 容易混淆\n\n" + "\n".join(confusions) +
        "\n\n## 记忆线索\n\n对每个知识点使用‘作用 - 条件 - 步骤 - 边界 - 例子’五格卡片，避免只背一句结论。\n\n"
        "## 自检清单\n\n- [ ] 能在 3 分钟内复述全部重点。\n- [ ] 能解释任意两个重点的区别和联系。\n- [ ] 能从题干识别适用知识点。\n- [ ] 能写出一个常见错误及修正方法。"
    )
    return body, {"format": "rapid_summary", "comparison_rows": len(rows)}


def _knowledge_graph(title: str, subject: str, points: list[str], _: list[tuple[str, str]], __: dict[str, Any]) -> tuple[str, dict[str, Any]]:
    lines = ["graph TD", f'  ROOT["{_mermaid_label(subject)}"]']
    for index, point in enumerate(points, start=1):
        node = f"P{index}"
        lines.extend([
            f'  ROOT -->|核心内容| {node}["{_mermaid_label(point)}"]',
            f'  {node} --> {node}A["作用与定义"]',
            f'  {node} --> {node}B["条件与步骤"]',
            f'  {node} --> {node}C["应用与边界"]',
        ])
        if index > 1:
            lines.append(f"  P{index - 1} -->|学习后进入| {node}")
    mermaid = "\n".join(lines)
    relation_lines = "\n".join(f"- **{point}**：按作用、条件、步骤、应用与边界向下展开。" for point in points)
    body = (
        f"# {title}\n\n```mermaid\n{mermaid}\n```\n\n"
        f"## 图谱说明\n\n图谱以 {subject} 为根节点，将核心知识点和每个知识点的理解维度分层展开。\n\n"
        "## 关系解读\n\n" + relation_lines +
        "\n\n## 学习顺序\n\n1. 从根节点确认总体目标。\n2. 按图中顺序学习各核心点。\n3. 对每个点依次回答作用、条件、步骤、应用和边界。\n4. 回到根节点，用自己的话复述知识结构。"
    )
    return body, {"format": "mermaid", "mermaid": mermaid, "node_count": 1 + len(points) * 4}


def _quiz_set(title: str, subject: str, points: list[str], context: list[tuple[str, str]], params: dict[str, Any]) -> tuple[str, dict[str, Any]]:
    count = {"short": 5, "medium": 8, "long": 12}.get(str(params.get("output_length") or "medium"), 8)
    questions = []
    answers = []
    explanations = []
    answer_labels = ["A", "B", "C", "D"]
    for index in range(1, count + 1):
        point = points[(index - 1) % len(points)]
        answer = answer_labels[(index - 1) % 4]
        correct_text = _short_evidence((index - 1) % max(1, len(context)), context)
        if correct_text == "回到课程资料定位定义、条件和步骤":
            correct_text = f"能说明 {point} 的作用、适用条件，并独立完成一次应用"
        distractors = [
            "只记住术语，不检查适用条件",
            "跳过推理过程，直接照抄结果",
            "只看答案，不进行独立练习",
        ]
        options = distractors[:]
        options.insert(answer_labels.index(answer), correct_text)
        questions.append(
            f"### 第 {index} 题 · {point}\n\n关于 **{point}**，下列哪一项最符合有效掌握或资料依据？\n\n" +
            "\n".join(f"{label}. {option}" for label, option in zip(answer_labels, options))
        )
        answers.append(f"{index}. {answer}")
        explanations.append(
            f"### 第 {index} 题逐项解析\n\n" +
            "\n".join(
                f"- **{label}**：{'正确。该项直接体现资料依据或完整掌握标准。' if label == answer else '错误。该做法缺少理解、条件判断或独立应用环节。'}"
                for label in answer_labels
            )
        )
    body = (
        f"# {title}\n\n## 练习说明\n\n本练习共 {count} 道四选一单项选择题，覆盖 {subject} 的重点内容。建议先独立作答，再查看解析。\n\n"
        "## 选择题\n\n" + "\n\n".join(questions) +
        "\n\n## 参考答案\n\n" + "；".join(answers) +
        "\n\n## 逐项解析\n\n" + "\n\n".join(explanations) +
        "\n\n## 复习建议\n\n把错题对应到知识点，写下错误原因；24 小时后遮住答案重做，并能解释四个选项才算完成。"
    )
    return body, {"format": "single_choice", "question_count": count, "answers": answers}


def _review_outline(title: str, subject: str, points: list[str], _: list[tuple[str, str]], __: dict[str, Any]) -> tuple[str, dict[str, Any]]:
    chapters = []
    for index, point in enumerate(points, start=1):
        priority = "A 必须掌握" if index <= max(1, len(points) // 2) else "B 重点理解"
        chapters.append(
            f"### {index}. {point} · {priority}\n\n"
            f"- [ ] **概念**：能准确解释 {point} 的作用和关键词。\n"
            f"- [ ] **方法**：能写出适用条件和完整步骤。\n"
            f"- [ ] **典型题**：至少完成 2 道基础题和 1 道变式题。\n"
            f"- [ ] **易错点**：记录一个错误判断及其修正依据。\n"
            f"- [ ] **验收标准**：脱离资料完成复述和应用。"
        )
    body = (
        f"# {title}\n\n## 复习范围\n\n本提纲覆盖 {subject} 的 {len(points)} 个重点：{'、'.join(points)}。\n\n"
        "## 优先级\n\n- **A 必须掌握**：会解释、会判断、会做题。\n- **B 重点理解**：能说明联系并完成基础应用。\n- **C 快速了解**：知道用途和使用边界。\n\n"
        "## 章节提纲\n\n" + "\n\n".join(chapters) +
        "\n\n## 必须会\n\n- [ ] 从题干判断应使用哪个知识点。\n- [ ] 解释选择该方法的依据。\n- [ ] 独立完成步骤并检查结果。\n\n"
        "## 复习顺序\n\n先做诊断题，再补 A 类内容，随后完成 B 类对比，最后用综合题串联全部重点。\n\n"
        "## 考前检查\n\n遮住资料，用一张纸画出知识结构；不会复述或无法举例的条目立即回看并再做一道题。"
    )
    return body, {"format": "priority_outline", "chapter_count": len(points)}


def _mistake_review(title: str, subject: str, points: list[str], _: list[tuple[str, str]], params: dict[str, Any]) -> tuple[str, dict[str, Any]]:
    mistakes = params.get("mistakes") if isinstance(params.get("mistakes"), list) else []
    if not mistakes:
        body = (
            f"# {title}\n\n## 错因诊断\n\n"
            f"尚未提供真实错题：当前“{subject}”学习空间还没有已提交的错题记录，因此系统不会把普通知识点虚构成错题。\n\n"
            "请先完成并提交一次学习测验；答错的选择题会自动进入这里，并保留原题、选项、你的答案、正确答案和逐项解析。\n\n"
            "## 订正步骤\n\n1. 完成当前空间中的学习测验。\n2. 提交答案并查看逐项解析。\n3. 再次生成错题整理资源。\n\n"
            "## 正确规则\n\n错题档案只采用真实作答记录，不根据关键词推测或补写。\n\n"
            "## 变式练习\n\n有真实错题后，系统会围绕对应知识点安排同类变式题。\n\n"
            "## 复测标准\n\n同类题连续两次独立答对，并能说明每个选项的判断依据。"
        )
        return body, {"format": "mistake_cards", "has_actual_mistakes": False, "card_count": 0}

    cards: list[str] = []
    covered_points: list[str] = []
    for index, mistake in enumerate(mistakes[:30], start=1):
        if not isinstance(mistake, dict):
            continue
        quiz_title = str(mistake.get("quiz_title") or "学习测验").strip()
        stem = str(mistake.get("stem") or "题干缺失").strip()
        student_answer = str(mistake.get("student_answer") or "未作答").strip()
        correct_answer = str(mistake.get("correct_answer") or "未记录").strip()
        analysis = str(mistake.get("analysis") or "请结合逐项解析确认判断条件。").strip()
        knowledge_points = mistake.get("knowledge_points") if isinstance(mistake.get("knowledge_points"), list) else []
        point_text = "、".join(str(item).strip() for item in knowledge_points if str(item).strip()) or subject
        covered_points.extend(str(item).strip() for item in knowledge_points if str(item).strip())
        options = mistake.get("options") if isinstance(mistake.get("options"), list) else []
        option_lines = "\n".join(f"  - {str(option).strip()}" for option in options if str(option).strip()) or "  - 暂无选项记录"
        explanations = mistake.get("option_explanations") if isinstance(mistake.get("option_explanations"), dict) else {}
        explanation_lines = "\n".join(
            f"  - **{str(label).strip()}**：{str(text).strip()}"
            for label, text in explanations.items() if str(text).strip()
        ) or f"  - {analysis}"
        cards.append(
            f"### 错题卡 {index} · {quiz_title} 第 {mistake.get('question_order') or index} 题\n\n"
            f"**原题**：{stem}\n\n**选项**：\n{option_lines}\n\n"
            f"- **我的答案**：{student_answer}\n"
            f"- **正确答案**：{correct_answer}\n"
            f"- **对应知识点**：{point_text}\n"
            f"- **错因诊断**：先判断是概念边界、审题条件还是排除过程出现偏差。结合本题表现，重点复查“{point_text}”的成立条件。\n"
            f"- **正确规则**：{analysis}\n\n"
            f"**逐项解析**：\n{explanation_lines}\n\n"
            "- **订正步骤**：遮住答案重做 → 写出每个选项的判断依据 → 对照逐项解析 → 总结错误触发点。\n"
            "- **变式练习**：改变题干中的一个条件后重新判断四个选项，并说明答案变化原因。\n"
            "- **复测标准**：24 小时后能独立选对，并解释其余三个选项为什么不成立。"
        )
    body = (
        f"# {title}\n\n> 本档案来自当前学习空间内已提交测验的真实错题，共 {len(cards)} 道。\n\n"
        "## 错因诊断\n\n错题整理按原题逐项对照作答，定位概念边界、审题条件或排除过程中的具体偏差。\n\n"
        + "\n\n".join(cards) +
        "\n\n## 订正步骤\n\n1. 不看答案重做。\n2. 标出第一次出现偏差的位置。\n3. 写下正确规则和触发条件。\n4. 完成一道变式题。\n\n"
        "## 复测标准\n\n连续两次独立答对、能够解释错误选项，并在新题中识别同类陷阱后，才将该错题标记为已掌握。"
    )
    return body, {
        "format": "mistake_cards",
        "has_actual_mistakes": True,
        "card_count": len(cards),
        "knowledge_points": list(dict.fromkeys(covered_points)),
    }


def _plan(title: str, subject: str, points: list[str], _: list[tuple[str, str]], params: dict[str, Any]) -> tuple[str, dict[str, Any]]:
    configured_days = params.get("days")
    try:
        days = max(1, min(int(configured_days), 30))
    except (TypeError, ValueError):
        days = {"short": 3, "medium": 7, "long": 14}.get(str(params.get("output_length") or "medium"), 7)
    stages = ["诊断与目标确认", *[f"掌握 {point}" for point in points], "综合练习", "错题复盘", "最终检查"]
    daily = []
    for day in range(1, days + 1):
        stage = stages[min(day - 1, len(stages) - 1)] if day <= len(stages) else "间隔复习与迁移练习"
        point = points[(day - 1) % len(points)]
        daily.append(
            f"### 第 {day} 天 · {stage}\n\n"
            f"- **学习任务**：围绕 {point} 完成概念复述、例题分析和独立练习。\n"
            f"- **建议时长**：{45 + (day % 3) * 15} 分钟。\n"
            f"- **学习产出**：一张 {point} 五格卡片和一份练习订正记录。\n"
            f"- **验收标准**：不看资料解释条件与步骤，独立完成一题并说明错项原因。"
        )
    body = (
        f"# {title}\n\n## 目标拆解\n\n总目标：在 {days} 天内建立 {subject} 的知识结构，并能完成解释、应用和订正。\n\n"
        "## 每日安排\n\n" + "\n\n".join(daily) +
        "\n\n## 学习任务与学习产出\n\n每天至少留下可检查的笔记卡、作答记录或错题订正，不以‘看完了’作为完成证据。\n\n"
        "## 验收标准\n\n- 知识点能独立复述。\n- 练习能写出完整依据。\n- 错题能说明错误触发点。\n- 综合任务达到 80% 以上正确率。\n\n"
        "## 调整规则\n\n当日验收未通过时，次日先安排 20 分钟补偿复习；连续两天提前完成时，再增加综合题，不盲目提前进度。"
    )
    return body, {"format": "daily_plan", "days": days, "stage_count": len(daily)}


def _case_task(title: str, subject: str, points: list[str], _: list[tuple[str, str]], __: dict[str, Any]) -> tuple[str, dict[str, Any]]:
    point_text = "、".join(points)
    body = (
        f"# {title}\n\n## 案例背景\n\n你需要在一个真实的 {subject} 学习或项目场景中，综合运用 {point_text} 解决问题，并向同伴说明决策依据。\n\n"
        "## 任务目标\n\n- 识别场景中的核心问题和约束。\n- 选择合适知识点并说明原因。\n- 形成可检查、可复用的解决方案。\n\n"
        "## 任务要求\n\n1. 写出问题分析和已知条件。\n2. 为每个关键决策标注所用知识点。\n3. 给出完整实施过程、结果与风险。\n4. 设计一个验证方案证明结果满足要求。\n\n"
        "## 交付物\n\n- 一页问题分析。\n- 一份步骤清晰的解决方案。\n- 一张知识点与决策对应表。\n- 一份结果验证和复盘记录。\n\n"
        "## 实施步骤\n\n需求澄清 → 知识点选择 → 方案设计 → 执行验证 → 复盘改进。\n\n"
        "## 评价标准\n\n| 维度 | 分值 | 达标要求 |\n|---|---:|---|\n| 问题分析 | 20 | 条件和约束完整 |\n| 知识运用 | 30 | 选择正确且能解释依据 |\n| 方案过程 | 25 | 步骤可执行、结果可验证 |\n| 复盘表达 | 15 | 能指出不足和改进方法 |\n| 规范性 | 10 | 结构清楚、引用准确 |\n\n"
        "## 提示\n\n先用最小方案验证关键判断，再补充复杂条件；遇到不确定结论时回到课程资料查证，不要用猜测替代依据。"
    )
    return body, {"format": "case_assignment", "rubric_total": 100}


def _context_entries(params: dict[str, Any]) -> list[tuple[str, str]]:
    raw = params.get("knowledge_context")
    if not isinstance(raw, list):
        return []
    entries = []
    for item in raw[:12]:
        if not isinstance(item, dict):
            continue
        source = str(item.get("source") or item.get("source_file_name") or "学习资料").strip()
        content = re.sub(r"\s+", " ", str(item.get("content") or item.get("chunk_text") or "")).strip()
        if content:
            entries.append((source, content[:700]))
    return entries


def _context_appendix(context: list[tuple[str, str]]) -> str:
    if not context:
        return ""
    sources = []
    excerpts = []
    for source, content in context[:8]:
        if source not in sources:
            sources.append(source)
        excerpts.append(f"> **{source}**：{content}")
    return "\n\n## 资料依据\n\n" + "\n\n".join(excerpts) + "\n\n来源文件：" + "、".join(sources)


def _evidence_for(index: int, context: list[tuple[str, str]]) -> str:
    if not context:
        return "当前未选择资料片段，请在学习时回到教材核对定义、条件和步骤。"
    source, content = context[index % len(context)]
    return f"《{source}》提到：{content[:260]}"


def _short_evidence(index: int, context: list[tuple[str, str]]) -> str:
    if not context:
        return "回到课程资料定位定义、条件和步骤"
    _, content = context[index % len(context)]
    return content[:90]


def _mermaid_label(value: str) -> str:
    return value.replace('"', "'").replace("\n", " ")[:36]
