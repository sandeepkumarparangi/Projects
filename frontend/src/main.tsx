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
      setError(caught instanceof Error ? caught.message : 'Analysis failed.');
    } finally {
      setLoading(false);
    }
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
