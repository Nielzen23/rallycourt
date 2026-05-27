type FeatureCardProps = {
  title: string
  description: string
}

function FeatureCard({ title, description }: FeatureCardProps) {
  return (
    <article className="grid gap-3 rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h3 className="text-xl font-semibold text-slate-900">{title}</h3>
      <p className="text-sm leading-7 text-slate-600">{description}</p>
    </article>
  )
}

export default FeatureCard
