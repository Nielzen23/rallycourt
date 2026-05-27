import type { ReactNode } from 'react'
import './PageLayout.css'

type PageLayoutProps = {
  eyebrow?: string
  title: string
  description: string
  children: ReactNode
}

function PageLayout({
  eyebrow,
  title,
  description,
  children,
}: PageLayoutProps) {
  return (
    <main className="page-layout">
      <header className="page-layout__header">
        {eyebrow ? <span className="page-layout__eyebrow">{eyebrow}</span> : null}
        <h1>{title}</h1>
        <p>{description}</p>
      </header>
      <section className="page-layout__content">{children}</section>
    </main>
  )
}

export default PageLayout
