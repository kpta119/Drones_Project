import { ClientDto } from "./client_dto";

export interface OperatorDto extends ClientDto {
  certificates: string[];
  coordinates: string;
  radius: number;
  description: string;
  portfolio: {
    id: number;
    title: string;
    photos: {
      id: number;
      name: string;
      photoUrl: string;
    }[];
  };
  operatorServices: {
    id: number;
    serviceName: string;
  }[];
}
