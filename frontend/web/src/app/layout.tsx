import "./globals.css";
import {
  Montserrat,
  Open_Sans,
  Nova_Mono,
  Lato,
  Fredoka,
} from "next/font/google";

const montserrat = Montserrat({
  subsets: ["latin"],
  variable: "--font-montserrat",
});
const openSans = Open_Sans({ subsets: ["latin"], variable: "--font-opensans" });
const novaMono = Nova_Mono({
  weight: "400",
  subsets: ["latin"],
  variable: "--font-novamono",
});
const lato = Lato({
  weight: ["100", "400", "700", "900"],
  subsets: ["latin"],
  variable: "--font-lato",
});
const fredoka = Fredoka({ subsets: ["latin"], variable: "--font-fredoka" });

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html
      lang="en"
      className={`${montserrat.variable} ${openSans.variable} ${novaMono.variable} ${lato.variable} ${fredoka.variable}`}
    >
      <body>{children}</body>
    </html>
  );
}
