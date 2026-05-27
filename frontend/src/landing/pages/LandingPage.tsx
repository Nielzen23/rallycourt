import LandingFooter from '../components/LandingFooter'
import LandingHeader from '../components/LandingHeader'
import HeroSection from '../components/HeroSection'
import FeatureCard from '../components/FeatureCard'
import MapPreview from '../components/MapPreview'
import HowItWorksSection from '../components/HowItWorksSection'

function LandingPage() {
  return (
    <main className="min-h-screen bg-slate-50 text-slate-900">
      <LandingHeader />
      <HeroSection />

      <section className="mx-auto grid w-full max-w-7xl gap-6 px-6 py-14 lg:grid-cols-3 lg:px-8">
        <FeatureCard
          description="View available courts with location details and map markers."
          title="Find Courts"
        />
        <FeatureCard
          description="Book fair game durations of 1 hour, 1.5 hours, or 2 hours."
          title="Reserve Slots"
        />
        <FeatureCard
          description="Reservations are held for 30 minutes while waiting for mock payment confirmation."
          title="Secure Your Booking"
        />
      </section>

      <section className="mx-auto grid w-full max-w-7xl gap-8 px-6 pb-14 lg:px-8">
        <MapPreview />
        <HowItWorksSection />
      </section>

      <LandingFooter />
    </main>
  )
}

export default LandingPage
