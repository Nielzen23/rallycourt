import logo from '../assets/RallyCourtLogo.png'

type HeaderLogoProps = {
  alt: string
  className?: string
}

function HeaderLogo({ alt, className = '' }: HeaderLogoProps) {
  return (
    <img
      className={`h-16 w-16 object-contain sm:h-20 sm:w-20 ${className}`.trim()}
      src={logo}
      alt={alt}
    />
  )
}

export default HeaderLogo
