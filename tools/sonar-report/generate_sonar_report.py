import os
import sys
import html
import json
from datetime import datetime
from pathlib import Path

import requests
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib import colors


SONAR_URL = os.getenv("SONAR_HOST_URL", "http://sonarqube:9000").rstrip("/")
PROJECT_KEY = os.getenv("SONAR_PROJECT_KEY", "mekano-api")
PROJECT_NAME = os.getenv("SONAR_PROJECT_NAME", "Mekano API")
SONAR_TOKEN = os.getenv("SONAR_TOKEN")

OUTPUT_DIR = Path("reports")
OUTPUT_DIR.mkdir(exist_ok=True)

if not SONAR_TOKEN:
    print("ERRO: SONAR_TOKEN não encontrado.")
    print("Execute sonar-init antes ou defina SONAR_TOKEN no ambiente.")
    sys.exit(1)


session = requests.Session()
session.auth = (SONAR_TOKEN, "")


def get_json(path, params=None):
    url = f"{SONAR_URL}{path}"
    response = session.get(url, params=params, timeout=60)
    response.raise_for_status()
    return response.json()


def get_measures():
    metric_keys = ",".join([
        "bugs",
        "vulnerabilities",
        "security_hotspots",
        "code_smells",
        "coverage",
        "duplicated_lines_density",
        "ncloc",
        "reliability_rating",
        "security_rating",
        "sqale_rating",
    ])

    return get_json(
        "/api/measures/component",
        {
            "component": PROJECT_KEY,
            "metricKeys": metric_keys,
        },
    )


def get_issues(issue_type):
    all_issues = []
    page = 1

    while True:
        data = get_json(
            "/api/issues/search",
            {
                "componentKeys": PROJECT_KEY,
                "types": issue_type,
                "resolved": "false",
                "ps": 500,
                "p": page,
            },
        )

        issues = data.get("issues", [])
        all_issues.extend(issues)

        paging = data.get("paging", {})
        total = paging.get("total", len(all_issues))
        page_size = paging.get("pageSize", 500)

        if page * page_size >= total:
            break

        page += 1

    return all_issues


def get_hotspots():
    all_hotspots = []
    page = 1

    while True:
        data = get_json(
            "/api/hotspots/search",
            {
                "projectKey": PROJECT_KEY,
                "ps": 500,
                "p": page,
            },
        )

        hotspots = data.get("hotspots", [])
        all_hotspots.extend(hotspots)

        paging = data.get("paging", {})
        total = paging.get("total", len(all_hotspots))
        page_size = paging.get("pageSize", 500)

        if page * page_size >= total:
            break

        page += 1

    return all_hotspots


def measures_to_dict(measures_payload):
    result = {}

    for measure in measures_payload.get("component", {}).get("measures", []):
        result[measure["metric"]] = measure.get("value", "N/A")

    return result


def rating_to_letter(value):
    mapping = {
        "1.0": "A",
        "2.0": "B",
        "3.0": "C",
        "4.0": "D",
        "5.0": "E",
        "1": "A",
        "2": "B",
        "3": "C",
        "4": "D",
        "5": "E",
    }
    return mapping.get(str(value), str(value))


def save_json(filename, payload):
    path = OUTPUT_DIR / filename
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")


def generate_html(measures, vulnerabilities, bugs, code_smells, hotspots):
    now = datetime.now().strftime("%d/%m/%Y %H:%M:%S")

    html_content = f"""
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8" />
  <title>Relatório SonarQube - {html.escape(PROJECT_NAME)}</title>
  <style>
    body {{
      font-family: Arial, sans-serif;
      margin: 40px;
      color: #222;
    }}
    h1, h2, h3 {{
      color: #1f3a5f;
    }}
    table {{
      border-collapse: collapse;
      width: 100%;
      margin-bottom: 24px;
    }}
    th, td {{
      border: 1px solid #ccc;
      padding: 8px;
      text-align: left;
      font-size: 13px;
    }}
    th {{
      background: #f0f3f7;
    }}
    .summary {{
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12px;
      margin-bottom: 24px;
    }}
    .card {{
      border: 1px solid #ccc;
      padding: 12px;
      border-radius: 6px;
      background: #fafafa;
    }}
    .metric {{
      font-size: 24px;
      font-weight: bold;
    }}
    .muted {{
      color: #666;
      font-size: 12px;
    }}
  </style>
</head>
<body>
  <h1>Relatório de Vulnerabilidades — {html.escape(PROJECT_NAME)}</h1>

  <p><strong>Projeto:</strong> {html.escape(PROJECT_KEY)}</p>
  <p><strong>SonarQube:</strong> {html.escape(SONAR_URL)}</p>
  <p><strong>Data de geração:</strong> {now}</p>

  <h2>1. Sumário Executivo</h2>

  <div class="summary">
    <div class="card"><div class="metric">{measures.get("bugs", "0")}</div><div>Bugs</div></div>
    <div class="card"><div class="metric">{measures.get("vulnerabilities", "0")}</div><div>Vulnerabilidades</div></div>
    <div class="card"><div class="metric">{measures.get("security_hotspots", "0")}</div><div>Security Hotspots</div></div>
    <div class="card"><div class="metric">{measures.get("code_smells", "0")}</div><div>Code Smells</div></div>
    <div class="card"><div class="metric">{measures.get("coverage", "N/A")}%</div><div>Cobertura</div></div>
    <div class="card"><div class="metric">{measures.get("duplicated_lines_density", "N/A")}%</div><div>Duplicação</div></div>
  </div>

  <h2>2. Métricas de Qualidade</h2>

  <table>
    <tr><th>Métrica</th><th>Resultado</th></tr>
    <tr><td>Linhas de código</td><td>{measures.get("ncloc", "N/A")}</td></tr>
    <tr><td>Bugs</td><td>{measures.get("bugs", "0")}</td></tr>
    <tr><td>Vulnerabilidades</td><td>{measures.get("vulnerabilities", "0")}</td></tr>
    <tr><td>Security Hotspots</td><td>{measures.get("security_hotspots", "0")}</td></tr>
    <tr><td>Code Smells</td><td>{measures.get("code_smells", "0")}</td></tr>
    <tr><td>Cobertura</td><td>{measures.get("coverage", "N/A")}%</td></tr>
    <tr><td>Duplicação</td><td>{measures.get("duplicated_lines_density", "N/A")}%</td></tr>
    <tr><td>Reliability Rating</td><td>{rating_to_letter(measures.get("reliability_rating", "N/A"))}</td></tr>
    <tr><td>Security Rating</td><td>{rating_to_letter(measures.get("security_rating", "N/A"))}</td></tr>
    <tr><td>Maintainability Rating</td><td>{rating_to_letter(measures.get("sqale_rating", "N/A"))}</td></tr>
  </table>

  <h2>3. Vulnerabilidades Abertas</h2>
  {issues_table_html(vulnerabilities)}

  <h2>4. Bugs Abertos</h2>
  {issues_table_html(bugs)}

  <h2>5. Security Hotspots</h2>
  {hotspots_table_html(hotspots)}

  <h2>6. Conclusão</h2>
  <p>
    Este relatório foi gerado automaticamente a partir da Web API do SonarQube.
    Ele consolida métricas de segurança, confiabilidade, manutenibilidade,
    vulnerabilidades, bugs e security hotspots do projeto analisado.
  </p>

  <p class="muted">
    Observação: o SonarQube Community não substitui uma análise de CVEs de dependências.
    Para isso, recomenda-se complementar com OWASP Dependency-Check ou Dependency-Track.
  </p>
</body>
</html>
"""

    path = OUTPUT_DIR / "mekano-sonar-report.html"
    path.write_text(html_content, encoding="utf-8")
    return path


def issues_table_html(issues):
    if not issues:
        return "<p>Nenhum item aberto encontrado.</p>"

    rows = """
    <table>
      <tr>
        <th>Severidade</th>
        <th>Regra</th>
        <th>Arquivo</th>
        <th>Linha</th>
        <th>Mensagem</th>
      </tr>
    """

    for issue in issues:
        component = issue.get("component", "")
        component = component.split(":", 1)[-1]
        rows += f"""
      <tr>
        <td>{html.escape(issue.get("severity", ""))}</td>
        <td>{html.escape(issue.get("rule", ""))}</td>
        <td>{html.escape(component)}</td>
        <td>{html.escape(str(issue.get("line", "")))}</td>
        <td>{html.escape(issue.get("message", ""))}</td>
      </tr>
        """

    rows += "</table>"
    return rows


def hotspots_table_html(hotspots):
    if not hotspots:
        return "<p>Nenhum security hotspot encontrado.</p>"

    rows = """
    <table>
      <tr>
        <th>Status</th>
        <th>Probabilidade</th>
        <th>Regra</th>
        <th>Arquivo</th>
        <th>Linha</th>
        <th>Mensagem</th>
      </tr>
    """

    for hotspot in hotspots:
        component = hotspot.get("component", "")
        component = component.split(":", 1)[-1]
        rows += f"""
      <tr>
        <td>{html.escape(hotspot.get("status", ""))}</td>
        <td>{html.escape(hotspot.get("vulnerabilityProbability", ""))}</td>
        <td>{html.escape(hotspot.get("ruleKey", ""))}</td>
        <td>{html.escape(component)}</td>
        <td>{html.escape(str(hotspot.get("line", "")))}</td>
        <td>{html.escape(hotspot.get("message", ""))}</td>
      </tr>
        """

    rows += "</table>"
    return rows


def generate_pdf(measures, vulnerabilities, bugs, code_smells, hotspots):
    pdf_path = OUTPUT_DIR / "mekano-sonar-report.pdf"

    doc = SimpleDocTemplate(
        str(pdf_path),
        pagesize=A4,
        rightMargin=36,
        leftMargin=36,
        topMargin=36,
        bottomMargin=36,
    )

    styles = getSampleStyleSheet()
    story = []

    story.append(Paragraph(f"Relatório de Vulnerabilidades — {PROJECT_NAME}", styles["Title"]))
    story.append(Spacer(1, 12))
    story.append(Paragraph(f"<b>Projeto:</b> {PROJECT_KEY}", styles["Normal"]))
    story.append(Paragraph(f"<b>SonarQube:</b> {SONAR_URL}", styles["Normal"]))
    story.append(Paragraph(f"<b>Data:</b> {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}", styles["Normal"]))
    story.append(Spacer(1, 18))

    story.append(Paragraph("1. Sumário Executivo", styles["Heading2"]))

    summary_data = [
        ["Métrica", "Resultado"],
        ["Bugs", measures.get("bugs", "0")],
        ["Vulnerabilidades", measures.get("vulnerabilities", "0")],
        ["Security Hotspots", measures.get("security_hotspots", "0")],
        ["Code Smells", measures.get("code_smells", "0")],
        ["Cobertura", f'{measures.get("coverage", "N/A")}%' if measures.get("coverage") else "N/A"],
        ["Duplicação", f'{measures.get("duplicated_lines_density", "N/A")}%' if measures.get("duplicated_lines_density") else "N/A"],
        ["Reliability Rating", rating_to_letter(measures.get("reliability_rating", "N/A"))],
        ["Security Rating", rating_to_letter(measures.get("security_rating", "N/A"))],
        ["Maintainability Rating", rating_to_letter(measures.get("sqale_rating", "N/A"))],
    ]

    story.append(make_table(summary_data, [220, 220]))
    story.append(Spacer(1, 18))

    story.append(Paragraph("2. Vulnerabilidades Abertas", styles["Heading2"]))
    story.append(make_issues_pdf_table(vulnerabilities))
    story.append(PageBreak())

    story.append(Paragraph("3. Bugs Abertos", styles["Heading2"]))
    story.append(make_issues_pdf_table(bugs))
    story.append(PageBreak())

    story.append(Paragraph("4. Security Hotspots", styles["Heading2"]))
    story.append(make_hotspots_pdf_table(hotspots))
    story.append(Spacer(1, 18))

    story.append(Paragraph("5. Conclusão", styles["Heading2"]))
    story.append(Paragraph(
        "Relatório gerado automaticamente a partir da Web API do SonarQube. "
        "Para análise específica de CVEs em dependências, recomenda-se complementar "
        "com OWASP Dependency-Check ou OWASP Dependency-Track.",
        styles["Normal"]
    ))

    doc.build(story)
    return pdf_path


def make_table(data, widths):
    table = Table(data, colWidths=widths, repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.lightgrey),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.grey),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("FONTSIZE", (0, 0), (-1, -1), 8),
    ]))
    return table


def make_issues_pdf_table(issues):
    if not issues:
        return Paragraph("Nenhum item aberto encontrado.", getSampleStyleSheet()["Normal"])

    data = [["Sev.", "Arquivo", "Linha", "Mensagem"]]

    for issue in issues[:100]:
        component = issue.get("component", "").split(":", 1)[-1]
        data.append([
            issue.get("severity", ""),
            component[-45:],
            str(issue.get("line", "")),
            issue.get("message", "")[:120],
        ])

    return make_table(data, [45, 150, 40, 260])


def make_hotspots_pdf_table(hotspots):
    if not hotspots:
        return Paragraph("Nenhum security hotspot encontrado.", getSampleStyleSheet()["Normal"])

    data = [["Status", "Prob.", "Arquivo", "Linha", "Mensagem"]]

    for hotspot in hotspots[:100]:
        component = hotspot.get("component", "").split(":", 1)[-1]
        data.append([
            hotspot.get("status", ""),
            hotspot.get("vulnerabilityProbability", ""),
            component[-40:],
            str(hotspot.get("line", "")),
            hotspot.get("message", "")[:100],
        ])

    return make_table(data, [60, 55, 140, 35, 210])


def main():
    print(f"Coletando dados do SonarQube: {SONAR_URL}")
    print(f"Projeto: {PROJECT_KEY}")

    measures_payload = get_measures()
    vulnerabilities = get_issues("VULNERABILITY")
    bugs = get_issues("BUG")
    code_smells = get_issues("CODE_SMELL")
    hotspots = get_hotspots()

    measures = measures_to_dict(measures_payload)

    save_json("sonar-measures.json", measures_payload)
    save_json("sonar-vulnerabilities.json", vulnerabilities)
    save_json("sonar-bugs.json", bugs)
    save_json("sonar-code-smells.json", code_smells)
    save_json("sonar-hotspots.json", hotspots)

    html_path = generate_html(measures, vulnerabilities, bugs, code_smells, hotspots)
    pdf_path = generate_pdf(measures, vulnerabilities, bugs, code_smells, hotspots)

    print("Relatórios gerados com sucesso:")
    print(f"- {html_path}")
    print(f"- {pdf_path}")
    print("- reports/sonar-measures.json")
    print("- reports/sonar-vulnerabilities.json")
    print("- reports/sonar-bugs.json")
    print("- reports/sonar-code-smells.json")
    print("- reports/sonar-hotspots.json")


if __name__ == "__main__":
    main()