import React, { FormEvent, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { AlertCircle, BadgeCheck, FileText, Loader2, Sparkles, UploadCloud } from 'lucide-react';
import './styles.css';

type Analysis = {
  atsScore: number;
  verdict: string;
  strengths: string[];
  improvements: string[];
  missingKeywords: string[];
  suggestedKeywords: string[];
  formattingTips: string[];
  summary: string;
  aiPowered: boolean;
  analysisSource: string;
  diagnostic: string | null;
};

const apiBase = import.meta.env.VITE_API_BASE_URL ?? '';

const sampleResumeText = `Summary
Full stack Java developer with Spring Boot, React, REST API, SQL, Docker, and CI/CD experience.

Experience
Built Spring Boot microservices and React dashboards for internal operations.
Improved API response time by 35% through query optimization and caching.
Automated deployments with GitHub Actions and Docker.

Skills
Java, Spring Boot, React, TypeScript, SQL, REST, Docker, testing, agile
`;

const sampleJobDescription = `We are hiring a Full Stack Java Developer to build APIs and internal dashboards.
The role needs Java, Spring Boot, React, TypeScript, REST APIs, SQL, Docker,
GitHub Actions, cloud deployment experience, testing, and secure coding practices.
Nice to have: observability, PostgreSQL, OAuth, and performance tuning.`;

function App() {
  const [resume, setResume] = useState<File | null>(null);
  const [jobDescription, setJobDescription] = useState('');
  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [elapsedMs, setElapsedMs] = useState<number | null>(null);

  const scoreLabel = useMemo(() => {
    if (!analysis) return 'Waiting for upload';
    if (analysis.atsScore >= 80) return 'Strong';
    if (analysis.atsScore >= 65) return 'Good';
    return 'Needs work';
  }, [analysis]);

  async function analyzeResume(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setAnalysis(null);
    setElapsedMs(null);

    if (!resume) {
      setError('Upload a PDF, DOCX, or TXT resume first.');
      return;
    }

    const formData = new FormData();
    formData.append('resume', resume);
    formData.append('jobDescription', jobDescription);

    setLoading(true);
    const startedAt = performance.now();
    try {
      const response = await fetch(`${apiBase}/api/resumes/analyze`, {
        method: 'POST',
        body: formData,
      });
      const contentType = response.headers.get('content-type') ?? '';
      const payload = contentType.includes('application/json') ? await response.json() : null;
      if (!response.ok) {
        throw new Error(payload?.message ?? `Analysis failed with HTTP ${response.status}.`);
      }
      if (!payload) {
        throw new Error('Analysis failed: backend returned an empty response.');
      }
      setAnalysis(payload);
      setElapsedMs(Math.round(performance.now() - startedAt));
    } catch (caught) {
      try {
        const resumeText = await readTextResume(resume);
        const fallback = analyzeInBrowser(resumeText, jobDescription);
        const message =
          caught instanceof TypeError
            ? 'Cannot reach the backend API.'
            : caught instanceof Error
              ? caught.message
              : 'Backend analysis failed.';
        setAnalysis({
          ...fallback,
          diagnostic: `${message} Showing browser demo analysis. Connect VITE_API_BASE_URL to a deployed Spring Boot backend for OpenAI-powered public results.`,
        });
        setElapsedMs(Math.round(performance.now() - startedAt));
      } catch (fallbackError) {
        setError(
          fallbackError instanceof Error
            ? fallbackError.message
            : 'Analysis failed. Check that the backend is running and reachable.',
        );
      }
    } finally {
      setLoading(false);
    }
  }

  function loadSampleAnalysis() {
    setResume(new File([sampleResumeText], 'sample-resume.txt', { type: 'text/plain' }));
    setJobDescription(sampleJobDescription);
    setAnalysis(null);
    setError('');
    setElapsedMs(null);
  }

  return (
    <main className="shell">
      <section className="workspace">
        <div className="intro">
          <div>
            <p className="eyebrow">AI Resume Analyzer</p>
            <h1>ATS scoring and keyword tuning for every application.</h1>
          </div>
          <div className="status-pill">
            <Sparkles size={18} />
            {analysis?.aiPowered ? 'OpenAI powered' : 'Local scoring ready'}
          </div>
        </div>

        <div className="layout">
          <form className="panel upload-panel" onSubmit={analyzeResume}>
            <label className="dropzone">
              <UploadCloud size={34} />
              <span>{resume ? resume.name : 'Upload resume'}</span>
              <small>PDF, DOCX, or TXT up to 5 MB</small>
              <input
                type="file"
                accept=".pdf,.docx,.txt"
                onChange={(event) => setResume(event.target.files?.[0] ?? null)}
              />
            </label>

            <label className="field">
              <span>Target job description</span>
              <textarea
                value={jobDescription}
                onChange={(event) => setJobDescription(event.target.value)}
                rows={10}
                placeholder="Paste the job description to compare required skills, tools, and responsibilities."
              />
            </label>

            <button className="secondary-button" type="button" onClick={loadSampleAnalysis} disabled={loading}>
              <Sparkles size={18} />
              Use sample resume and job
            </button>

            {error && (
              <div className="error">
                <AlertCircle size={18} />
                {error}
              </div>
            )}

            <button className="primary-button" type="submit" disabled={loading}>
              {loading ? <Loader2 className="spin" size={19} /> : <FileText size={19} />}
              {loading ? 'Analyzing fast...' : 'Analyze resume'}
            </button>
          </form>

          <section className="panel results-panel">
            <div className="score-card">
              <div className="score-ring" style={{ '--score': analysis?.atsScore ?? 0 } as React.CSSProperties}>
                <strong>{analysis?.atsScore ?? '--'}</strong>
                <span>ATS</span>
              </div>
              <div>
                <p className="eyebrow">{scoreLabel}</p>
                <h2>{analysis?.verdict ?? 'Ready when you are'}</h2>
                <p>{analysis?.summary ?? 'Upload a resume and optional job description to see the score, gaps, and next edits.'}</p>
                {elapsedMs !== null && (
                  <p className="timing">
                    Completed in {(elapsedMs / 1000).toFixed(1)}s using {analysis?.analysisSource ?? 'local'} review.
                  </p>
                )}
                {analysis?.diagnostic && <p className="diagnostic">{analysis.diagnostic}</p>}
              </div>
            </div>

            {analysis ? (
              <div className="result-grid">
                <ResultList title="Strengths" items={analysis.strengths} positive />
                <ResultList title="Improvements" items={analysis.improvements} />
                <KeywordList title="Missing keywords" items={analysis.missingKeywords} empty="No critical gaps found." />
                <KeywordList title="Suggested keywords" items={analysis.suggestedKeywords} empty="Paste a job description for richer keywords." />
                <ResultList title="Formatting tips" items={analysis.formattingTips} />
              </div>
            ) : (
              <div className="empty-state">
                <BadgeCheck size={28} />
                <p>Your analysis will appear here with recruiter-friendly recommendations.</p>
              </div>
            )}
          </section>
        </div>
      </section>
    </main>
  );
}

async function readTextResume(file: File) {
  if (file.type.includes('text') || file.name.toLowerCase().endsWith('.txt')) {
    return file.text();
  }
  throw new Error('Backend is unavailable. Browser demo mode can only analyze TXT resumes.');
}

function analyzeInBrowser(resumeText: string, jobDescription: string): Analysis {
  const coreKeywords = [
    'java',
    'spring boot',
    'react',
    'sql',
    'api',
    'rest',
    'microservices',
    'aws',
    'docker',
    'kubernetes',
    'ci/cd',
    'testing',
    'agile',
  ];
  const roleKeywords = [
    'typescript',
    'postgresql',
    'mongodb',
    'security',
    'observability',
    'cloud',
    'github actions',
    'junit',
    'oauth',
    'kafka',
    'redis',
    'terraform',
    'linux',
    'html',
    'css',
    'javascript',
    'node',
    'performance',
    'accessibility',
  ];
  const actionVerbs = ['built', 'created', 'delivered', 'improved', 'reduced', 'increased', 'automated', 'designed'];
  const stopWords = new Set([
    'and',
    'the',
    'for',
    'with',
    'from',
    'that',
    'this',
    'are',
    'you',
    'our',
    'will',
    'have',
    'has',
    'using',
    'into',
    'your',
    'their',
    'they',
    'job',
    'role',
    'team',
    'work',
    'build',
    'develop',
    'candidate',
    'experience',
    'hiring',
    'looking',
    'needs',
    'nice',
    'internal',
  ]);

  const normalizedResume = resumeText.toLowerCase();
  const normalizedJob = jobDescription.toLowerCase();
  const strengths: string[] = [];
  const improvements: string[] = [];
  const formattingTips: string[] = [];
  let score = 45;

  if (coreKeywords.some((term) => normalizedResume.includes(term))) {
    score += 12;
    strengths.push('Includes technical keywords that ATS systems commonly parse.');
  }
  if (actionVerbs.some((term) => normalizedResume.includes(term))) {
    score += 10;
    strengths.push('Uses action-oriented language in experience bullets.');
  } else {
    improvements.push('Start more bullets with action verbs such as built, automated, improved, or delivered.');
  }
  if (/(\d+%|\$\d+|\d+x|\d+\+)/.test(normalizedResume)) {
    score += 12;
    strengths.push('Mentions measurable impact, which helps recruiters compare outcomes.');
  } else {
    improvements.push('Add metrics like latency reduced, revenue supported, users served, or defect reduction.');
  }
  if (normalizedResume.includes('experience') && normalizedResume.includes('skills')) {
    score += 8;
    strengths.push('Has recognizable sections for experience and skills.');
  } else {
    formattingTips.push('Use clear ATS-friendly headings: Summary, Skills, Experience, Projects, Education.');
  }
  if (resumeText.length < 1800) {
    improvements.push('Add more role-specific detail; the resume text looks brief for an ATS scan.');
  }

  const suggestedKeywords = new Set<string>();
  const missingKeywords = new Set<string>();
  [...coreKeywords, ...roleKeywords].filter((term) => normalizedJob.includes(term)).forEach((term) => suggestedKeywords.add(term));
  normalizedJob
    .split(/[^a-z0-9+#./-]+/)
    .map((term) => term.trim().replace(/^[^a-z0-9+#]+|[^a-z0-9+#]+$/g, ''))
    .filter((term) => term.length >= 4)
    .filter((term) => !stopWords.has(term))
    .filter((term) => !/^\d+$/.test(term))
    .filter((term) => !term.endsWith('ing'))
    .slice(0, 12)
    .forEach((term) => suggestedKeywords.add(term));

  suggestedKeywords.forEach((term) => {
    const singular = term.endsWith('s') && term.length > 3 ? term.slice(0, -1) : term;
    if (!normalizedResume.includes(term) && !normalizedResume.includes(singular)) {
      missingKeywords.add(term);
    }
  });
  coreKeywords.forEach((term) => suggestedKeywords.add(term));

  const matchedKeywords = Math.max(0, suggestedKeywords.size - missingKeywords.size);
  score += Math.min(16, matchedKeywords * 2);

  formattingTips.push('Keep layout simple: avoid text boxes, icons-as-labels, tables for core experience, and tiny fonts.');
  formattingTips.push('Mirror the job description wording naturally where your experience supports it.');

  const atsScore = Math.max(0, Math.min(missingKeywords.size === 0 ? 100 : 92, score));
  const verdict = atsScore >= 80 ? 'Strong ATS match' : atsScore >= 65 ? 'Good foundation' : 'Needs targeted improvement';

  return {
    atsScore,
    verdict,
    strengths: strengths.length ? strengths : ['Resume has readable text and can be parsed by the analyzer.'],
    improvements: improvements.length ? improvements : ['Tune keywords for the exact job description before applying.'],
    missingKeywords: [...missingKeywords],
    suggestedKeywords: [...suggestedKeywords].slice(0, 14),
    formattingTips,
    summary: 'Browser demo analysis completed. Deploy the Spring Boot backend for OpenAI-powered review.',
    aiPowered: false,
    analysisSource: 'browser-demo',
    diagnostic: null,
  };
}

function ResultList({ title, items, positive = false }: { title: string; items: string[]; positive?: boolean }) {
  return (
    <article className="mini-panel">
      <h3>{title}</h3>
      <ul className={positive ? 'positive-list' : ''}>
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </article>
  );
}

function KeywordList({ title, items, empty }: { title: string; items: string[]; empty: string }) {
  return (
    <article className="mini-panel">
      <h3>{title}</h3>
      {items.length ? (
        <div className="keyword-cloud">
          {items.map((item) => (
            <span key={item}>{item}</span>
          ))}
        </div>
      ) : (
        <p className="muted">{empty}</p>
      )}
    </article>
  );
}

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
