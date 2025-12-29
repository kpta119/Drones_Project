export interface ClientDto {
  id: string;
  username: string;
  name: string;
  surname: string;
  email: string;
  phoneNumber: string;
  role: "CLIENT" | "OPERATOR";
  reviews: ReviewDto[];
  rating: number;
}

export interface ReviewDto {
  id: number;
  body: string;
  stars: number;
  authorId: string;
}
